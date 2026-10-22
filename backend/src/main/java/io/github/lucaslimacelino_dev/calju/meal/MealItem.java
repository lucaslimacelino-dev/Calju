package io.github.lucaslimacelino_dev.calju.meal;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Item do molde: qual alimento e quantos gramas, na ordem {@code position}. */
@Embeddable
public class MealItem {

    @Column(nullable = false)
    private UUID id;

    @Column(name = "food_id", nullable = false)
    private UUID foodId;

    @Column(name = "quantity_g", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantityG;

    @Column(nullable = false)
    private int position;

    protected MealItem() {
    }

    public MealItem(UUID id, UUID foodId, BigDecimal quantityG, int position) {
        this.id = id;
        this.foodId = foodId;
        this.quantityG = quantityG;
        this.position = position;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFoodId() {
        return foodId;
    }

    public BigDecimal getQuantityG() {
        return quantityG;
    }

    public int getPosition() {
        return position;
    }
}
