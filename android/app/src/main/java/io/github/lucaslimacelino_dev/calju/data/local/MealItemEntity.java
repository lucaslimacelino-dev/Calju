package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;

/**
 * Item de uma refeição salva. {@code foodId} aponta para o catálogo sem chave estrangeira,
 * para que reimportar o catálogo não apague os itens das refeições.
 */
@Entity(tableName = "meal_items",
        foreignKeys = @ForeignKey(entity = MealEntity.class, parentColumns = "id", childColumns = "meal_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("meal_id")})
public class MealItemEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "meal_id")
    @NonNull
    public String mealId;

    @ColumnInfo(name = "food_id")
    @NonNull
    public String foodId;

    @ColumnInfo(name = "quantity_g")
    @NonNull
    public BigDecimal quantityG;

    public int position;

    public MealItemEntity(@NonNull String id, @NonNull String mealId, @NonNull String foodId,
                          @NonNull BigDecimal quantityG, int position) {
        this.id = id;
        this.mealId = mealId;
        this.foodId = foodId;
        this.quantityG = quantityG;
        this.position = position;
    }
}
