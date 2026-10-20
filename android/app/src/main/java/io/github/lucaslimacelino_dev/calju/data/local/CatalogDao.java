package io.github.lucaslimacelino_dev.calju.data.local;

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

    @Query("SELECT f.id, f.name, f.search_name AS searchName, f.category, "
            + "p.label AS portionLabel, p.grams AS portionGrams "
            + "FROM foods f LEFT JOIN food_portions p ON p.food_id = f.id AND p.is_default = 1")
    List<FoodSearchRow> allForSearch();

    @Query("SELECT * FROM foods WHERE id = :id")
    FoodEntity food(String id);

    @Query("SELECT * FROM food_nutrients WHERE food_id = :foodId")
    List<FoodNutrientEntity> nutrientsOf(String foodId);

    @Query("SELECT * FROM food_portions WHERE food_id = :foodId AND is_default = 1 LIMIT 1")
    FoodPortionEntity defaultPortionOf(String foodId);
}
