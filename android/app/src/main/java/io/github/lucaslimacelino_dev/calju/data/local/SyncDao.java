package io.github.lucaslimacelino_dev.calju.data.local;

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

    // Leitura para aplicar o que veio do servidor

    @Query("SELECT * FROM log_entries WHERE id = :id")
    LogEntryEntity entry(String id);

    @Query("SELECT * FROM meals WHERE id = :id")
    MealEntity meal(String id);

    @Query("SELECT * FROM nutrition_goals WHERE id = :id")
    NutritionGoalEntity goal(String id);

    @Query("SELECT * FROM nutrition_goals WHERE effective_from = :date")
    NutritionGoalEntity goalOn(LocalDate date);

    @Query("SELECT * FROM foods WHERE id = :id")
    FoodEntity food(String id);

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
