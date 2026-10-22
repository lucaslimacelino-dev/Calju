package io.github.lucaslimacelino_dev.calju.food;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Porção padrão do alimento, ex.: "1 xícara" = 40 g. */
@Embeddable
public class FoodPortion {

    @Column(nullable = false, length = 100)
    private String label;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal grams;

    protected FoodPortion() {
    }

    public FoodPortion(String label, BigDecimal grams) {
        this.label = label;
        this.grams = grams;
    }

    public String getLabel() {
        return label;
    }

    public BigDecimal getGrams() {
        return grams;
    }
}
