package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

import java.math.BigDecimal;

/** Cópia do valor por 100 g de um nutriente no momento do registro. */
@Entity(tableName = "log_item_nutrients",
        primaryKeys = {"log_item_id", "nutrient_code"},
        foreignKeys = @ForeignKey(entity = LogItemEntity.class, parentColumns = "id", childColumns = "log_item_id",
                onDelete = ForeignKey.CASCADE))
public class LogItemNutrientEntity {

    @ColumnInfo(name = "log_item_id")
    @NonNull
    public String logItemId;

    @ColumnInfo(name = "nutrient_code")
    @NonNull
    public String nutrientCode;

    @ColumnInfo(name = "amount_per_100g")
    @Nullable
    public BigDecimal amountPer100g;

    @NonNull
    public String qualifier;

    public LogItemNutrientEntity(@NonNull String logItemId, @NonNull String nutrientCode,
                                 @Nullable BigDecimal amountPer100g, @NonNull String qualifier) {
        this.logItemId = logItemId;
        this.nutrientCode = nutrientCode;
        this.amountPer100g = amountPer100g;
        this.qualifier = qualifier;
    }
}
