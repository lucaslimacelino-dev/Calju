package io.github.lucaslimacelino_dev.calju.data.sync;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.ItemWithNutrients;
import io.github.lucaslimacelino_dev.calju.data.local.LogItemNutrientEntity;
import io.github.lucaslimacelino_dev.calju.data.local.NutritionGoalEntity;
import io.github.lucaslimacelino_dev.calju.data.local.SyncDao;
import io.github.lucaslimacelino_dev.calju.data.local.SyncStates;
import io.github.lucaslimacelino_dev.calju.data.remote.CaljuApi;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.SyncDtos;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Conflitos que o usuário decide (registros do diário e metas): mostra a versão deste aparelho e
 * a da conta e aplica a escolha. Buscar a versão da conta precisa de rede.
 */
public class ConflictResolver {

    public enum Kind {
        ENTRY,
        GOAL
    }

    public record Item(String name, BigDecimal grams) {
    }

    /** Um lado do conflito. {@code missing}: não existe nesse lado. Metas usam kcal e macros; registros, itens e kcal. */
    public record Side(boolean missing, boolean deleted, List<Item> items, BigDecimal kcal, BigDecimal proteinG,
                       BigDecimal carbsG, BigDecimal fatG) {

        static Side absent() {
            return new Side(true, false, List.of(), null, null, null, null);
        }
    }

    /** {@code occasion} só nos registros do diário. As cópias do servidor ficam guardadas para aplicar a escolha. */
    public record Choice(Kind kind, String id, LocalDate date, Occasion occasion, Side local, Side remote,
                         SyncDtos.LogEntry remoteEntry, SyncDtos.Goal remoteGoal) {
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CaljuDatabase database;
    private final SyncDao sync;
    private final CaljuApi api;
    private final SyncWriter writer;

    public ConflictResolver(CaljuDatabase database, CaljuApi api) {
        this.database = database;
        this.sync = database.syncDao();
        this.api = api;
        this.writer = new SyncWriter(database, Clock.systemUTC());
    }

    public LiveData<Integer> conflictCount() {
        return sync.observeConflictCount();
    }

    public LiveData<Integer> failedCount() {
        return sync.observeFailedCount();
    }

    /** Recusados pelo servidor voltam a ser enviados (ex.: depois de uma correção no app). */
    @WorkerThread
    public void retryFailed() {
        database.runInTransaction(() -> {
            sync.retryFailedFoods();
            sync.retryFailedMeals();
            sync.retryFailedGoals();
            sync.retryFailedEntries();
        });
    }

    /** O próximo conflito, já com a versão da conta; nulo = nenhum. */
    @WorkerThread
    public Choice next() throws IOException {
        List<EntryWithItems> entries = sync.conflictEntries();
        if (!entries.isEmpty()) {
            EntryWithItems local = entries.get(0);
            SyncDtos.LogEntry remote = fetch(api.logEntry(local.entry.id));
            return new Choice(Kind.ENTRY, local.entry.id, local.entry.logDate, local.entry.occasion, side(local),
                    side(remote), remote, null);
        }
        List<NutritionGoalEntity> goals = sync.conflictGoals();
        if (!goals.isEmpty()) {
            NutritionGoalEntity local = goals.get(0);
            SyncDtos.Goal remote = remoteGoal(local);
            return new Choice(Kind.GOAL, local.id, local.effectiveFrom, null,
                    new Side(false, false, List.of(), local.kcal, local.proteinG, local.carbsG, local.fatG),
                    remote == null ? Side.absent() : new Side(false, false, List.of(), remote.kcal(),
                            remote.proteinG(), remote.carbohydrateG(), remote.fatG()),
                    null, remote);
        }
        return null;
    }

    /** Fica a versão deste aparelho: ela é enviada de novo, agora sobre a versão atual da conta. */
    @WorkerThread
    public void keepLocal(Choice choice) {
        if (choice.kind() == Kind.ENTRY) {
            sync.entryRebase(choice.id(), choice.remoteEntry() == null ? 0 : choice.remoteEntry().version());
            return;
        }
        SyncDtos.Goal remote = choice.remoteGoal();
        if (remote == null) {
            sync.goalRebase(choice.id(), 0);
        } else if (remote.id().equals(choice.id())) {
            sync.goalRebase(choice.id(), remote.version());
        } else {
            // Outra meta no mesmo dia: os valores daqui passam a ocupar a meta da conta.
            database.runInTransaction(() -> {
                NutritionGoalEntity local = sync.goal(choice.id());
                sync.deleteGoal(choice.id());
                NutritionGoalEntity merged = new NutritionGoalEntity(remote.id(), local.effectiveFrom, local.kcal,
                        local.proteinG, local.carbsG, local.fatG, local.updatedAt, local.version + 1,
                        SyncStates.PENDING);
                merged.serverVersion = remote.version();
                sync.deleteGoal(remote.id());
                database.goalDao().insert(merged);
            });
        }
    }

    /** Fica a versão da conta: ela substitui a deste aparelho. */
    @WorkerThread
    public void useServer(Choice choice) {
        database.runInTransaction(() -> {
            if (choice.kind() == Kind.ENTRY) {
                sync.deleteEntry(choice.id());
                if (choice.remoteEntry() != null) {
                    writer.entry(choice.remoteEntry());
                }
            } else {
                sync.deleteGoal(choice.id());
                if (choice.remoteGoal() != null) {
                    writer.goal(choice.remoteGoal());
                }
            }
        });
    }

    private SyncDtos.Goal remoteGoal(NutritionGoalEntity local) throws IOException {
        SyncDtos.Goal byId = fetch(api.goal(local.id));
        if (byId != null) {
            return byId;
        }
        List<SyncDtos.Goal> all = fetch(api.goals());
        if (all != null) {
            for (SyncDtos.Goal remote : all) {
                if (remote.effectiveFrom().equals(local.effectiveFrom.toString())) {
                    return remote;
                }
            }
        }
        return null;
    }

    /** Nulo se o servidor não tem o registro. */
    private static <T> T fetch(Call<T> call) throws IOException {
        Response<T> response = call.execute();
        if (response.code() == 404) {
            return null;
        }
        if (!response.isSuccessful() || response.body() == null) {
            throw new IOException("HTTP " + response.code());
        }
        return response.body();
    }

    private static Side side(EntryWithItems local) {
        List<Item> items = new ArrayList<>();
        BigDecimal kcal = BigDecimal.ZERO;
        for (ItemWithNutrients item : local.items) {
            items.add(new Item(item.item.foodNameSnap, item.item.quantityG));
            for (LogItemNutrientEntity value : item.nutrients) {
                if (Nutrients.ENERGY.equals(value.nutrientCode) && value.amountPer100g != null) {
                    kcal = kcal.add(value.amountPer100g.multiply(item.item.quantityG));
                }
            }
        }
        return new Side(false, local.entry.deletedAt != null, items, per100(kcal), null, null, null);
    }

    private static Side side(SyncDtos.LogEntry remote) {
        if (remote == null) {
            return Side.absent();
        }
        List<Item> items = new ArrayList<>();
        BigDecimal kcal = BigDecimal.ZERO;
        for (SyncDtos.LogItem item : remote.items()) {
            items.add(new Item(item.foodName(), item.quantityG()));
            for (SyncDtos.NutrientValue value : item.nutrients()) {
                if (Nutrients.ENERGY.equals(value.code()) && value.amount() != null) {
                    kcal = kcal.add(value.amount().multiply(item.quantityG()));
                }
            }
        }
        return new Side(false, remote.deletedAt() != null, items, per100(kcal), null, null, null);
    }

    private static BigDecimal per100(BigDecimal sum) {
        return sum.divide(HUNDRED, 0, RoundingMode.HALF_UP);
    }
}
