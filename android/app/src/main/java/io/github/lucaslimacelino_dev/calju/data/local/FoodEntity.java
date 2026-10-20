package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "foods")
public class FoodEntity {

    /** UUID em texto; os alimentos da TACO têm UUID fixo, gerado pelo catalog-builder. */
    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String name;

    /** Nome sem acentos, minúsculo e sem pontuação, usado na busca. */
    @ColumnInfo(name = "search_name")
    @NonNull
    public String searchName;

    @Nullable
    public String category;

    @ColumnInfo(name = "source_id")
    @Nullable
    public String sourceId;

    @ColumnInfo(name = "source_number")
    @Nullable
    public Integer sourceNumber;

    public FoodEntity(@NonNull String id, @NonNull String name, @NonNull String searchName,
                      @Nullable String category, @Nullable String sourceId, @Nullable Integer sourceNumber) {
        this.id = id;
        this.name = name;
        this.searchName = searchName;
        this.category = category;
        this.sourceId = sourceId;
        this.sourceNumber = sourceNumber;
    }
}
