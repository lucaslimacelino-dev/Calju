package io.github.lucaslimacelino_dev.calju.data.sync;

import androidx.annotation.WorkerThread;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.function.Supplier;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogDao;
import io.github.lucaslimacelino_dev.calju.data.local.DiaryDao;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.FoodEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodPortionEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogEntryEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogItemEntity;
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
 * cursor. Nunca sobrescreve em silêncio: 409 marca conflito; mudança local ainda não enviada não
 * é trocada pela do servidor. Roda fora da thread da tela.
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
    private final DiaryDao diary;
    private final CatalogDao catalogDao;
    private final CatalogRepository catalog;
    private final CaljuApi api;
    private final SessionManager session;
    private final SyncPrefs prefs;
    private int sent;
    private int received;
    private int conflicts;

    public SyncEngine(CaljuDatabase database, CatalogRepository catalog, CaljuApi api, SessionManager session,
                      SyncPrefs prefs) {
        this.database = database;
        this.sync = database.syncDao();
        this.diary = database.diaryDao();
        this.catalogDao = database.catalogDao();
        this.catalog = catalog;
        this.api = api;
        this.session = session;
        this.prefs = prefs;
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
            String state = stateFor(response);
            if (state == null) {
                sync.foodSent(food.id, response.body().version(), food.version);
                sent++;
            } else {
                sync.foodState(food.id, state);
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
            String state = stateFor(response);
            if (state == null) {
                sync.mealSent(m.id, response.body().version(), m.version);
                sent++;
            } else {
                sync.mealState(m.id, state);
            }
        }
    }

    private void pushGoals() throws Stop {
        for (NutritionGoalEntity goal : sync.pendingGoals()) {
            Response<SyncDtos.Goal> response = execute(() -> api.putGoal(goal.id, SyncMapper.goalBody(goal)));
            String state = stateFor(response);
            if (state == null) {
                sync.goalSent(goal.id, response.body().version(), goal.version);
                sent++;
            } else {
                sync.goalState(goal.id, state);
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
            String state = stateFor(response);
            if (state == null) {
                sync.entrySent(e.id, response.body().version(), e.version);
                sent++;
            } else {
                sync.entryState(e.id, state);
            }
        }
    }

    /** Nulo = aceito. 409/404 = conflito (o servidor tem outra versão ou não tem o registro). */
    private String stateFor(Response<?> response) {
        if (response.isSuccessful() && response.body() != null) {
            return null;
        }
        if (response.code() == 409 || response.code() == 404) {
            conflicts++;
            return SyncStates.CONFLICT;
        }
        return SyncStates.FAILED;
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
                sync.deleteFood(dto.id());
                catalogDao.insertFood(SyncMapper.food(dto));
                catalogDao.insertFoodNutrients(SyncMapper.foodValues(dto));
                FoodPortionEntity portion = SyncMapper.foodPortion(dto);
                if (portion != null) {
                    catalogDao.insertPortions(Collections.singletonList(portion));
                }
                received++;
            }
        }
        for (SyncDtos.Meal dto : page.meals()) {
            MealEntity local = sync.meal(dto.id());
            if (shouldApply(local == null ? null : local.syncState, local == null ? 0 : local.serverVersion, dto.version())) {
                sync.deleteMeal(dto.id());
                database.mealDao().insertMeal(SyncMapper.meal(dto));
                database.mealDao().insertItems(SyncMapper.mealItems(dto));
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
            sync.deleteGoal(dto.id());
            database.goalDao().insert(SyncMapper.goal(dto));
            received++;
        }
        for (SyncDtos.LogEntry dto : page.logEntries()) {
            LogEntryEntity local = sync.entry(dto.id());
            if (shouldApply(local == null ? null : local.syncState, local == null ? 0 : local.serverVersion, dto.version())) {
                sync.deleteEntry(dto.id());
                diary.insertEntry(SyncMapper.entry(dto));
                for (LogItemEntity item : SyncMapper.entryItems(dto)) {
                    diary.insertItem(item);
                }
                diary.insertItemNutrients(SyncMapper.entryItemNutrients(dto));
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
