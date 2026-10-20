package io.github.lucaslimacelino_dev.calju.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(
        entities = {
                NutrientEntity.class,
                FoodEntity.class,
                FoodNutrientEntity.class,
                FoodPortionEntity.class,
                CatalogInfoEntity.class
        },
        version = 1)
@TypeConverters(Converters.class)
public abstract class CaljuDatabase extends RoomDatabase {

    public static final String FILE_NAME = "calju.db";

    public abstract CatalogDao catalogDao();

    public static CaljuDatabase create(Context context) {
        return Room.databaseBuilder(context.getApplicationContext(), CaljuDatabase.class, FILE_NAME).build();
    }
}
