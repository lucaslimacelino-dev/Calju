package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public enum NutrientUnit {
    KCAL("kcal", 0),
    GRAM("g", 1),
    MILLIGRAM("mg", 0),
    MICROGRAM("µg", 0);

    private final String symbol;
    private final int displayScale;

    NutrientUnit(String symbol, int displayScale) {
        this.symbol = symbol;
        this.displayScale = displayScale;
    }

    public String symbol() {
        return symbol;
    }

    public BigDecimal roundForDisplay(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        return value.setScale(displayScale, RoundingMode.HALF_UP);
    }
}
