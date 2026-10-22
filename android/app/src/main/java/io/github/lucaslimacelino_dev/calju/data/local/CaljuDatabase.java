package io.github.lucaslimacelino_dev.calju.data.local;

import android.content.Context;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Versão 2: tabelas do diário. Versão 3: metas. Versão 4: refeições salvas.
 * Versão 5: alimentos do usuário (marca, exclusão lógica e controle de sincronização em foods).
 * As migrações são geradas pelo Room comparando
 * os esquemas exportados em app/schemas.
 */
@Database(
        entities = {
                NutrientEntity.class,
                FoodEntity.class,
                FoodNutrientEntity.class,
                FoodPortionEntity.class,
                CatalogInfoEntity.class,
                LogEntryEntity.class,
                LogItemEntity.class,
                LogItemNutrientEntity.class,
                NutritionGoalEntity.class,
                MealEntity.class,
                MealItemEntity.class
        },
        version = 5,
        autoMigrations = {
                @AutoMigration(from = 1, to = 2),
                @AutoMigration(from = 2, to = 3),
                @AutoMigration(from = 3, to = 4),
                @AutoMigration(from = 4, to = 5)
        })
@TypeConverters(Converters.class)
public abstract class CaljuDatabase extends RoomDatabase {

    public static final String FILE_NAME = "calju.db";

    public abstract CatalogDao catalogDao();

    public abstract DiaryDao diaryDao();

    public abstract GoalDao goalDao();

    public abstract MealDao mealDao();

    public static CaljuDatabase create(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), CaljuDatabase.class, FILE_NAME).build();
    }
}
