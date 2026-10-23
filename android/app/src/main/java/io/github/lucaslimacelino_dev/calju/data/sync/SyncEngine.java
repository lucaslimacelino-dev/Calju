package io.github.lucaslimacelino_dev.calju.data.sync;

import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogDao;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.FoodEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogEntryEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.NutritionGoalEntity;
import io.github.lucaslimacelino_dev.calju.data.local.SyncDao;
import io.github.lucaslimacelino_dev.calju.data.local.SyncStates;
import io.github.lucaslimacelino_dev.calju.data.remote.CaljuApi;
import io.github.lucaslimacelino_dev.calju.data.remote.SessionManager;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.SyncDtos;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Sincroniza o aparelho com a conta: primeiro envia o que mudou aqui (PUT/DELETE por registro,
 * com a versão do servidor que o aparelho conhece), depois recebe o que mudou no servidor pelo
 * cursor. Nunca sobrescreve em silêncio. No conflito (409/404) busca a cópia do servidor: conteúdo
 * igual → só adota a versão; alimento ou refeição diferente → a versão do servidor fica e a do
 * aparelho vira uma cópia "(conflito)"; registro do diário ou meta diferente → fica em conflito
 * para o usuário escolher. Mudança local ainda não enviada não é trocada pela do servidor.
 * Roda fora da thread da tela.
 */
public class SyncEngine {

    static final int PAGE_SIZE = 500;

    public enum Outcome {
        DONE,
        SIGNED_OUT,
        OFFLINE,
        SERVER_ERROR
    }

    /** {@code conflicts}: registros que ficaram para o usuário decidir nesta rodada. */
    public record Result(Outcome outcome, int sent, int received, int conflicts) {
    }

    /** Para interromper a rodada sem rede ou sem sessão (os registros ficam pendentes). */
    private static final class Stop extends Exception {
        final Outcome outcome;

        Stop(Outcome outcome) {
            super(outcome.name());
            this.outcome = outcome;
        }
    }

    private final CaljuDatabase database;
    private final SyncDao sync;
    private final CatalogDao catalogDao;
    private final CatalogRepository catalog;
    private final CaljuApi api;
    private final SessionManager session;
    private final SyncPrefs prefs;
    private final SyncWriter writer;
    private int sent;
    private int received;
    private int conflicts;

    public SyncEngine(CaljuDatabase database, CatalogRepository catalog, CaljuApi api, SessionManager session,
                      SyncPrefs prefs) {
        this.database = database;
        this.sync = database.syncDao();
        this.catalogDao = database.catalogDao();
        this.catalog = catalog;
        this.api = api;
        this.session = session;
        this.prefs = prefs;
        this.writer = new SyncWriter(database, Clock.systemUTC());
    }

    @WorkerThread
    public synchronized Result run() {
        sent = 0;
        received = 0;
        conflicts = 0;
        if (!session.isSignedIn()) {
            return new Result(Outcome.SIGNED_OUT, 0, 0, 0);
        }
        try {
            pushFoods();
            pushMeals();
            pushGoals();
            pushEntries();
            pull();
            return new Result(Outcome.DONE, sent, received, conflicts);
        } catch (Stop stop) {
            return new Result(stop.outcome, sent, received, conflicts);
        }
    }

    // Envio. Ordem: alimentos e refeições antes dos registros que podem citá-los.

    private void pushFoods() throws Stop {
        for (FoodEntity food : sync.pendingFoods()) {
            if (food.deletedAt != null && food.serverVersion == 0) {
                sync.foodSent(food.id, 0, food.version);
                continue;
            }
            Response<SyncDtos.Food> response = execute(food.deletedAt != null
                    ? () -> api.deleteFood(food.id, food.serverVersion)
                    : () -> api.putFood(food.id, SyncMapper.foodBody(food, catalogDao.nutrientsOf(food.id),
                    catalogDao.defaultPortionOf(food.id))));
            if (accepted(response)) {
                sync.foodSent(food.id, response.body().version(), food.version);
                sent++;
            } else if (isConflict(response)) {
                resolveFood(food);
            } else {
                sync.foodState(food.id, SyncStates.FAILED);
            }
        }
    }

    private void pushMeals() throws Stop {
        for (MealWithItems meal : sync.pendingMeals()) {
            MealEntity m = meal.meal;
            if ((m.deletedAt != null || meal.items.isEmpty()) && m.serverVersion == 0) {
                sync.mealSent(m.id, 0, m.version);
                continue;
            }
            Response<SyncDtos.Meal> response = execute(m.deletedAt != null
                    ? () -> api.deleteMeal(m.id, m.serverVersion)
                    : () -> api.putMeal(m.id, SyncMapper.mealBody(meal)));
            if (accepted(response)) {
                sync.mealSent(m.id, response.body().version(), m.version);
                sent++;
            } else if (isConflict(response)) {
                resolveMeal(meal);
            } else {
                sync.mealState(m.id, SyncStates.FAILED);
            }
        }
    }

    private void pushGoals() throws Stop {
        for (NutritionGoalEntity goal : sync.pendingGoals()) {
            Response<SyncDtos.Goal> response = execute(() -> api.putGoal(goal.id, SyncMapper.goalBody(goal)));
            if (accepted(response)) {
                sync.goalSent(goal.id, response.body().version(), goal.version);
                sent++;
            } else if (isConflict(response)) {
                resolveGoal(goal);
            } else {
                sync.goalState(goal.id, SyncStates.FAILED);
            }
        }
    }

    private void pushEntries() throws Stop {
        for (EntryWithItems entry : sync.pendingEntries()) {
            LogEntryEntity e = entry.entry;
            if ((e.deletedAt != null || entry.items.isEmpty()) && e.serverVersion == 0) {
                sync.entrySent(e.id, 0, e.version);
                continue;
            }
            Response<SyncDtos.LogEntry> response = execute(e.deletedAt != null
                    ? () -> api.deleteLogEntry(e.id, e.serverVersion)
                    : () -> api.putLogEntry(e.id, SyncMapper.entryBody(entry)));
            if (accepted(response)) {
                sync.entrySent(e.id, response.body().version(), e.version);
                sent++;
            } else if (isConflict(response)) {
                resolveEntry(entry);
            } else {
                sync.entryState(e.id, SyncStates.FAILED);
            }
        }
    }

    private static boolean accepted(Response<?> response) {
        return response.isSuccessful() && response.body() != null;
    }

    /** 409: o servidor tem outra versão. 404: o servidor não tem o registro. */
    private static boolean isConflict(Response<?> response) {
        return response.code() == 409 || response.code() == 404;
    }

    // Conflitos no envio

    private void resolveFood(FoodEntity food) throws Stop {
        SyncDtos.Food remote = fetch(() -> api.food(food.id));
        if (remote == null) {
            notOnServer(food.serverVersion, () -> sync.foodRebase(food.id, 0),
                    () -> sync.foodState(food.id, SyncStates.FAILED));
            return;
        }
        boolean deleted = food.deletedAt != null;
        if (SyncMapper.sameFood(SyncMapper.foodBody(food, catalogDao.nutrientsOf(food.id),
                catalogDao.defaultPortionOf(food.id)), deleted, remote)) {
            sync.foodSent(food.id, remote.version(), food.version);
            return;
        }
        database.runInTransaction(() -> {
            if (!deleted) {
                writer.copyFood(food);
            }
            writer.food(remote);
        });
        catalog.invalidateSearch();
    }

    private void resolveMeal(MealWithItems meal) throws Stop {
        MealEntity m = meal.meal;
        SyncDtos.Meal remote = fetch(() -> api.meal(m.id));
        if (remote == null) {
            notOnServer(m.serverVersion, () -> sync.mealRebase(m.id, 0),
                    () -> sync.mealState(m.id, SyncStates.FAILED));
            return;
        }
        boolean deleted = m.deletedAt != null;
        if (SyncMapper.sameMeal(SyncMapper.mealBody(meal), deleted, remote)) {
            sync.mealSent(m.id, remote.version(), m.version);
            return;
        }
        database.runInTransaction(() -> {
            if (!deleted && !meal.items.isEmpty()) {
                writer.copyMeal(meal);
            }
            writer.meal(remote);
        });
    }

    private void resolveGoal(NutritionGoalEntity goal) throws Stop {
        SyncDtos.Goal remote = remoteGoal(goal);
        if (remote == null) {
            notOnServer(goal.serverVersion, () -> sync.goalRebase(goal.id, 0),
                    () -> sync.goalState(goal.id, SyncStates.FAILED));
            return;
        }
        if (!SyncMapper.sameGoal(goal, remote)) {
            sync.goalState(goal.id, SyncStates.CONFLICT);
            conflicts++;
        } else if (remote.id().equals(goal.id)) {
            sync.goalSent(goal.id, remote.version(), goal.version);
        } else {
            // A mesma meta criada em dois aparelhos: fica a do servidor.
            database.runInTransaction(() -> {
                sync.deleteGoal(goal.id);
                writer.goal(remote);
            });
        }
    }

    private void resolveEntry(EntryWithItems entry) throws Stop {
        LogEntryEntity e = entry.entry;
        SyncDtos.LogEntry remote = fetch(() -> api.logEntry(e.id));
        if (remote == null) {
            notOnServer(e.serverVersion, () -> sync.entryRebase(e.id, 0),
                    () -> sync.entryState(e.id, SyncStates.FAILED));
            return;
        }
        if (SyncMapper.sameEntry(SyncMapper.entryBody(entry), e.deletedAt != null, remote)) {
            sync.entrySent(e.id, remote.version(), e.version);
        } else {
            sync.entryState(e.id, SyncStates.CONFLICT);
            conflicts++;
        }
    }

    /**
     * A meta do servidor com o mesmo id ou, se não houver, a que começa no mesmo dia (o servidor
     * aceita uma meta por dia).
     */
    private SyncDtos.Goal remoteGoal(NutritionGoalEntity goal) throws Stop {
        SyncDtos.Goal byId = fetch(() -> api.goal(goal.id));
        if (byId != null) {
            return byId;
        }
        Response<List<SyncDtos.Goal>> all = execute(api::goals);
        if (!accepted(all)) {
            throw new Stop(Outcome.SERVER_ERROR);
        }
        for (SyncDtos.Goal remote : all.body()) {
            if (remote.effectiveFrom().equals(goal.effectiveFrom.toString())) {
                return remote;
            }
        }
        return null;
    }

    /**
     * O servidor não tem o registro. Se o aparelho achava que tinha (versão > 0), ele volta a ser
     * novo e é criado no próximo envio; se já era novo, o id é de outra conta: fica recusado.
     */
    private static void notOnServer(int serverVersion, Runnable rebase, Runnable fail) {
        if (serverVersion > 0) {
            rebase.run();
        } else {
            fail.run();
        }
    }

    /** Cópia atual do servidor; nula se ele não tem o registro. */
    private <T> T fetch(Supplier<Call<T>> call) throws Stop {
        Response<T> response = execute(call);
        if (response.code() == 404) {
            return null;
        }
        if (!accepted(response)) {
            throw new Stop(Outcome.SERVER_ERROR);
        }
        return response.body();
    }

    // Recebimento

    private void pull() throws Stop {
        boolean foodsChanged = false;
        long cursor = prefs.cursor();
        while (true) {
            final long from = cursor;
            Response<SyncDtos.Pull> response = execute(() -> api.pull(from, PAGE_SIZE));
            if (!response.isSuccessful() || response.body() == null) {
                throw new Stop(Outcome.SERVER_ERROR);
            }
            SyncDtos.Pull page = response.body();
            foodsChanged |= !page.foods().isEmpty();
            database.runInTransaction(() -> apply(page));
            cursor = page.cursor();
            prefs.saveCursor(cursor);
            if (!page.hasMore()) {
                break;
            }
        }
        if (foodsChanged) {
            catalog.invalidateSearch();
        }
    }

    private void apply(SyncDtos.Pull page) {
        for (SyncDtos.Food dto : page.foods()) {
            FoodEntity local = sync.food(dto.id());
            if (shouldApply(local == null ? null : local.syncState, local == null ? 0 : local.serverVersion, dto.version())) {
                writer.food(dto);
                received++;
            }
        }
        for (SyncDtos.Meal dto : page.meals()) {
            MealEntity local = sync.meal(dto.id());
            if (shouldApply(local == null ? null : local.syncState, local == null ? 0 : local.serverVersion, dto.version())) {
                writer.meal(dto);
                received++;
            }
        }
        for (SyncDtos.Goal dto : page.goals()) {
            NutritionGoalEntity local = sync.goal(dto.id());
            if (!shouldApply(local == null ? null : local.syncState, local == null ? 0 : local.serverVersion, dto.version())) {
                continue;
            }
            NutritionGoalEntity sameDay = sync.goalOn(LocalDate.parse(dto.effectiveFrom()));
            if (sameDay != null && !sameDay.id.equals(dto.id())) {
                if (SyncStates.SYNCED.equals(sameDay.syncState)) {
                    sync.deleteGoal(sameDay.id);
                } else {
                    // Meta do aparelho, ainda não enviada, no mesmo dia: o usuário decide.
                    sync.goalState(sameDay.id, SyncStates.CONFLICT);
                    conflicts++;
                    continue;
                }
            }
            writer.goal(dto);
            received++;
        }
        for (SyncDtos.LogEntry dto : page.logEntries()) {
            LogEntryEntity local = sync.entry(dto.id());
            if (shouldApply(local == null ? null : local.syncState, local == null ? 0 : local.serverVersion, dto.version())) {
                writer.entry(dto);
                received++;
            }
        }
    }

    /**
     * Aplica a versão do servidor se o aparelho não tem o registro, ou tem uma cópia sincronizada
     * mais antiga. Mudança local pendente (ou em conflito) nunca é sobrescrita aqui.
     */
    private static boolean shouldApply(String localState, int localServerVersion, int remoteVersion) {
        if (localState == null) {
            return true;
        }
        if (!SyncStates.SYNCED.equals(localState)) {
            return false;
        }
        return remoteVersion > localServerVersion;
    }

    private <T> Response<T> execute(Supplier<Call<T>> call) throws Stop {
        try {
            Response<T> response = call.get().execute();
            if (response.code() == 401) {
                throw new Stop(Outcome.SIGNED_OUT);
            }
            if (response.code() >= 500) {
                throw new Stop(Outcome.SERVER_ERROR);
            }
            return response;
        } catch (IOException e) {
            throw new Stop(Outcome.OFFLINE);
        }
    }
}
