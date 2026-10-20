package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.math.BigDecimal;

/** Valor de um nutriente por 100 g. Linha ausente = desconhecido; TRACE = traço (amount nulo). */
@Entity(tableName = "food_nutrients",
        primaryKeys = {"food_id", "nutrient_code"},
        foreignKeys = {
                @ForeignKey(entity = FoodEntity.class, parentColumns = "id", childColumns = "food_id",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = NutrientEntity.class, parentColumns = "code", childColumns = "nutrient_code")
        },
        indices = {@Index("nutrient_code")})
public class FoodNutrientEntity {

    @ColumnInfo(name = "food_id")
    @NonNull
    public String foodId;

    @ColumnInfo(name = "nutrient_code")
    @NonNull
    public String nutrientCode;

    @ColumnInfo(name = "amount_per_100g")
    @Nullable
    public BigDecimal amountPer100g;

    /** "VALUE" ou "TRACE". */
    @NonNull
    public String qualifier;

    public FoodNutrientEntity(@NonNull String foodId, @NonNull String nutrientCode,
                              @Nullable BigDecimal amountPer100g, @NonNull String qualifier) {
        this.foodId = foodId;
        this.nutrientCode = nutrientCode;
        this.amountPer100g = amountPer100g;
        this.qualifier = qualifier;
    }
}
