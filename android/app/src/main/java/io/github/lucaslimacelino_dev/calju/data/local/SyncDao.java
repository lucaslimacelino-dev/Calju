package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;

import java.time.LocalDate;
import java.util.List;

/**
 * Consultas da sincronização. "Synced" só vale se o registro não mudou de novo enquanto era
 * enviado ({@code version} igual à lida antes do envio); senão ele continua pendente, já com a
 * versão nova do servidor como base.
 */
@Dao
public interface SyncDao {

    // Pendentes de envio

    @Transaction
    @Query("SELECT * FROM log_entries WHERE sync_state = 'PENDING'")
    List<EntryWithItems> pendingEntries();

    @Transaction
    @Query("SELECT * FROM meals WHERE sync_state = 'PENDING'")
    List<MealWithItems> pendingMeals();

    @Query("SELECT * FROM nutrition_goals WHERE sync_state = 'PENDING'")
    List<NutritionGoalEntity> pendingGoals();

    @Query("SELECT * FROM foods WHERE is_custom = 1 AND sync_state = 'PENDING'")
    List<FoodEntity> pendingFoods();

    // Envio confirmado

    @Query("UPDATE log_entries SET server_version = :serverVersion, sync_state = CASE WHEN version = :sentVersion "
            + "THEN 'SYNCED' ELSE sync_state END WHERE id = :id")
    void entrySent(String id, int serverVersion, int sentVersion);

    @Query("UPDATE meals SET server_version = :serverVersion, sync_state = CASE WHEN version = :sentVersion "
            + "THEN 'SYNCED' ELSE sync_state END WHERE id = :id")
    void mealSent(String id, int serverVersion, int sentVersion);

    @Query("UPDATE nutrition_goals SET server_version = :serverVersion, sync_state = CASE WHEN version = :sentVersion "
            + "THEN 'SYNCED' ELSE sync_state END WHERE id = :id")
    void goalSent(String id, int serverVersion, int sentVersion);

    @Query("UPDATE foods SET server_version = :serverVersion, sync_state = CASE WHEN version = :sentVersion "
            + "THEN 'SYNCED' ELSE sync_state END WHERE id = :id")
    void foodSent(String id, int serverVersion, int sentVersion);

    // Estado (conflito ou recusa)

    @Query("UPDATE log_entries SET sync_state = :state WHERE id = :id")
    void entryState(String id, String state);

    @Query("UPDATE meals SET sync_state = :state WHERE id = :id")
    void mealState(String id, String state);

    @Query("UPDATE nutrition_goals SET sync_state = :state WHERE id = :id")
    void goalState(String id, String state);

    @Query("UPDATE foods SET sync_state = :state WHERE id = :id")
    void foodState(String id, String state);

    // Nova base: a versão do servidor passa a ser {@code serverVersion} e o registro volta a ser
    // enviado (0 = o servidor não tem o registro; o envio o cria).

    @Query("UPDATE log_entries SET server_version = :serverVersion, sync_state = 'PENDING' WHERE id = :id")
    void entryRebase(String id, int serverVersion);

    @Query("UPDATE meals SET server_version = :serverVersion, sync_state = 'PENDING' WHERE id = :id")
    void mealRebase(String id, int serverVersion);

    @Query("UPDATE nutrition_goals SET server_version = :serverVersion, sync_state = 'PENDING' WHERE id = :id")
    void goalRebase(String id, int serverVersion);

    @Query("UPDATE foods SET server_version = :serverVersion, sync_state = 'PENDING' WHERE id = :id")
    void foodRebase(String id, int serverVersion);

    // Conflitos para o usuário decidir e registros recusados

    @Transaction
    @Query("SELECT * FROM log_entries WHERE sync_state = 'CONFLICT' ORDER BY log_date, created_at")
    List<EntryWithItems> conflictEntries();

    @Query("SELECT * FROM nutrition_goals WHERE sync_state = 'CONFLICT' ORDER BY effective_from")
    List<NutritionGoalEntity> conflictGoals();

    @Query("SELECT (SELECT COUNT(*) FROM log_entries WHERE sync_state = 'CONFLICT') "
            + "+ (SELECT COUNT(*) FROM nutrition_goals WHERE sync_state = 'CONFLICT')")
    LiveData<Integer> observeConflictCount();

    @Query("SELECT (SELECT COUNT(*) FROM log_entries WHERE sync_state = 'FAILED') "
            + "+ (SELECT COUNT(*) FROM meals WHERE sync_state = 'FAILED') "
            + "+ (SELECT COUNT(*) FROM nutrition_goals WHERE sync_state = 'FAILED') "
            + "+ (SELECT COUNT(*) FROM foods WHERE is_custom = 1 AND sync_state = 'FAILED')")
    LiveData<Integer> observeFailedCount();

    @Query("UPDATE log_entries SET sync_state = 'PENDING' WHERE sync_state = 'FAILED'")
    void retryFailedEntries();

    @Query("UPDATE meals SET sync_state = 'PENDING' WHERE sync_state = 'FAILED'")
    void retryFailedMeals();

    @Query("UPDATE nutrition_goals SET sync_state = 'PENDING' WHERE sync_state = 'FAILED'")
    void retryFailedGoals();

    @Query("UPDATE foods SET sync_state = 'PENDING' WHERE is_custom = 1 AND sync_state = 'FAILED'")
    void retryFailedFoods();

    // Leitura para aplicar o que veio do servidor

    @Query("SELECT * FROM log_entries WHERE id = :id")
    LogEntryEntity entry(String id);

    @Query("SELECT * FROM meals WHERE id = :id")
    MealEntity meal(String id);

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :id")
    MealWithItems mealWithItems(String id);

    @Transaction
    @Query("SELECT * FROM log_entries WHERE id = :id")
    EntryWithItems entryWithItems(String id);

    @Query("SELECT * FROM nutrition_goals WHERE id = :id")
    NutritionGoalEntity goal(String id);

    @Query("SELECT * FROM nutrition_goals WHERE effective_from = :date")
    NutritionGoalEntity goalOn(LocalDate date);

    @Query("SELECT * FROM foods WHERE id = :id")
    FoodEntity food(String id);

    // Dados do usuário no aparelho (o catálogo da TACO não conta e nunca é apagado)

    @Query("SELECT (SELECT COUNT(*) FROM log_entries) + (SELECT COUNT(*) FROM meals) "
            + "+ (SELECT COUNT(*) FROM nutrition_goals) + (SELECT COUNT(*) FROM foods WHERE is_custom = 1)")
    int userDataCount();

    @Query("SELECT (SELECT COUNT(*) FROM log_entries WHERE sync_state = 'PENDING') "
            + "+ (SELECT COUNT(*) FROM meals WHERE sync_state = 'PENDING') "
            + "+ (SELECT COUNT(*) FROM nutrition_goals WHERE sync_state = 'PENDING') "
            + "+ (SELECT COUNT(*) FROM foods WHERE is_custom = 1 AND sync_state = 'PENDING')")
    int pendingCount();

    @Query("DELETE FROM log_entries")
    void deleteAllEntries();

    @Query("DELETE FROM meals")
    void deleteAllMeals();

    @Query("DELETE FROM nutrition_goals")
    void deleteAllGoals();

    @Query("DELETE FROM foods WHERE is_custom = 1")
    void deleteAllCustomFoods();

    // Remoção antes de gravar a versão do servidor (os filhos saem em cascata)

    @Query("DELETE FROM log_entries WHERE id = :id")
    void deleteEntry(String id);

    @Query("DELETE FROM meals WHERE id = :id")
    void deleteMeal(String id);

    @Query("DELETE FROM nutrition_goals WHERE id = :id")
    void deleteGoal(String id);

    @Query("DELETE FROM foods WHERE id = :id")
    void deleteFood(String id);
}
