package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Um alimento consumido em determinada quantidade. */
public record Intake(Food food, BigDecimal grams) {

    public Intake {
        Objects.requireNonNull(food, "food");
        Objects.requireNonNull(grams, "grams");
        if (grams.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade consumida deve ser maior que zero: " + grams);
        }
    }
}
