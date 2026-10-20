package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;

@Entity(tableName = "food_portions",
        foreignKeys = @ForeignKey(entity = FoodEntity.class, parentColumns = "id", childColumns = "food_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("food_id")})
public class FoodPortionEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "food_id")
    @NonNull
    public String foodId;

    @NonNull
    public String label;

    @NonNull
    public BigDecimal grams;

    /** "REFERENCE" (tabela publicada) ou "USER". */
    @NonNull
    public String source;

    @ColumnInfo(name = "source_id")
    public String sourceId;

    @ColumnInfo(name = "is_default")
    public boolean isDefault;

    public FoodPortionEntity(@NonNull String id, @NonNull String foodId, @NonNull String label,
                             @NonNull BigDecimal grams, @NonNull String source, String sourceId, boolean isDefault) {
        this.id = id;
        this.foodId = foodId;
        this.label = label;
        this.grams = grams;
        this.source = source;
        this.sourceId = sourceId;
        this.isDefault = isDefault;
    }
}
