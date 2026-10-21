package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface MealDao {

    @Insert
    void insertMeal(MealEntity meal);

    @Insert
    void insertItems(List<MealItemEntity> items);

    @Transaction
    @Query("SELECT * FROM meals WHERE deleted_at IS NULL ORDER BY name COLLATE NOCASE")
    LiveData<List<MealWithItems>> observeAll();

    @Transaction
    @Query("SELECT * FROM meals WHERE deleted_at IS NULL ORDER BY name COLLATE NOCASE")
    List<MealWithItems> all();

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :id")
    MealWithItems byId(String id);

    @Query("UPDATE meals SET name = :name WHERE id = :id")
    void rename(String id, String name);

    @Query("DELETE FROM meal_items WHERE meal_id = :mealId")
    void deleteItems(String mealId);

    @Query("UPDATE meals SET updated_at = :now, version = version + 1, sync_state = '"
            + LogEntryEntity.SYNC_PENDING + "' WHERE id = :id")
    void touch(String id, long now);

    @Query("UPDATE meals SET deleted_at = :now WHERE id = :id")
    void markDeleted(String id, long now);

    @Query("UPDATE meals SET deleted_at = NULL WHERE id = :id")
    void clearDeleted(String id);
}
