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
 * Versão 6: server_version (última versão confirmada pelo servidor) nas tabelas sincronizadas.
 * Versão 7: unidade ("g" ou "ml") em foods e log_items.
 * Versão 8: nome do dia a dia e lista de comuns em foods; peso da porção no registro.
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
        version = 8,
        autoMigrations = {
                @AutoMigration(from = 1, to = 2),
                @AutoMigration(from = 2, to = 3),
                @AutoMigration(from = 3, to = 4),
                @AutoMigration(from = 4, to = 5),
                @AutoMigration(from = 5, to = 6),
                @AutoMigration(from = 6, to = 7),
                @AutoMigration(from = 7, to = 8)
        })
@TypeConverters(Converters.class)
public abstract class CaljuDatabase extends RoomDatabase {

    public static final String FILE_NAME = "calju.db";

    public abstract CatalogDao catalogDao();

    public abstract DiaryDao diaryDao();

    public abstract GoalDao goalDao();

    public abstract MealDao mealDao();

    public abstract SyncDao syncDao();

    public static CaljuDatabase create(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), CaljuDatabase.class, FILE_NAME).build();
    }
}
