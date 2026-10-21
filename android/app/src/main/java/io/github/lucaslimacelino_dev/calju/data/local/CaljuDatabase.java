package io.github.lucaslimacelino_dev.calju.data.local;

import android.content.Context;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

/**
 * Versão 2: tabelas do diário. Versão 3: metas. As migrações são geradas pelo Room comparando
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
                NutritionGoalEntity.class
        },
        version = 3,
        autoMigrations = {@AutoMigration(from = 1, to = 2), @AutoMigration(from = 2, to = 3)})
@TypeConverters(Converters.class)
public abstract class CaljuDatabase extends RoomDatabase {

    public static final String FILE_NAME = "calju.db";

    public abstract CatalogDao catalogDao();

    public abstract DiaryDao diaryDao();

    public abstract GoalDao goalDao();

    public static CaljuDatabase create(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), CaljuDatabase.class, FILE_NAME).build();
    }
}
