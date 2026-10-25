package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CatalogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertNutrients(List<NutrientEntity> nutrients);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFoods(List<FoodEntity> foods);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFoodNutrients(List<FoodNutrientEntity> values);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPortions(List<FoodPortionEntity> portions);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveCatalogInfo(CatalogInfoEntity info);

    @Query("SELECT version FROM catalog_info WHERE id = " + CatalogInfoEntity.SINGLE_ROW_ID)
    Integer catalogVersion();

    @Query("SELECT COUNT(*) FROM foods")
    int countFoods();

    @Query("SELECT COUNT(*) FROM nutrients")
    int countNutrients();

    @Query("SELECT COUNT(*) FROM food_portions")
    int countPortions();

    @Query("SELECT f.id, f.name, f.search_name AS searchName, f.category, f.brand, "
            + "p.label AS portionLabel, p.grams AS portionGrams, f.display_name AS displayName, "
            + "f.is_common AS common, f.is_custom AS custom "
            + "FROM foods f LEFT JOIN food_portions p ON p.food_id = f.id AND p.is_default = 1 "
            + "WHERE f.deleted_at IS NULL")
    List<FoodSearchRow> allForSearch();

    @Query("SELECT * FROM nutrients ORDER BY display_order")
    List<NutrientEntity> allNutrients();

    @Query("SELECT * FROM foods WHERE id = :id")
    FoodEntity food(String id);

    @Query("SELECT * FROM food_nutrients WHERE food_id = :foodId")
    List<FoodNutrientEntity> nutrientsOf(String foodId);

    @Query("SELECT * FROM food_portions WHERE food_id = :foodId AND is_default = 1 LIMIT 1")
    FoodPortionEntity defaultPortionOf(String foodId);

    @Query("SELECT * FROM foods WHERE is_custom = 1 AND deleted_at IS NULL ORDER BY search_name")
    LiveData<List<FoodEntity>> observeCustomFoods();

    @Insert
    void insertFood(FoodEntity food);

    @Query("UPDATE foods SET name = :name, search_name = :searchName, brand = :brand, updated_at = :now, "
            + "version = version + 1, sync_state = 'PENDING' WHERE id = :id AND is_custom = 1")
    void updateCustomFood(String id, String name, String searchName, String brand, long now);

    @Query("UPDATE foods SET unit = :unit WHERE id = :id AND is_custom = 1")
    void setCustomFoodUnit(String id, String unit);

    @Query("DELETE FROM food_nutrients WHERE food_id = :foodId")
    void deleteNutrientsOf(String foodId);

    @Query("DELETE FROM food_portions WHERE food_id = :foodId")
    void deletePortionsOf(String foodId);

    @Query("UPDATE foods SET deleted_at = :deletedAt, updated_at = :now, version = version + 1, "
            + "sync_state = 'PENDING' WHERE id = :id AND is_custom = 1")
    void setCustomFoodDeleted(String id, Long deletedAt, long now);
}
