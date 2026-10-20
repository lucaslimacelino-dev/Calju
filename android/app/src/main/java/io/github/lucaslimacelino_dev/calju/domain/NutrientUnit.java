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

    public static NutrientUnit fromSymbol(String symbol) {
        for (NutrientUnit unit : values()) {
            if (unit.symbol.equals(symbol)) {
                return unit;
            }
        }
        throw new IllegalArgumentException("Unidade desconhecida: " + symbol);
    }

    /**
     * kcal inteiro; g com 1 casa; mg e µg inteiros a partir de 10 e com 1 casa abaixo disso
     * (ex.: cobre 0,2 mg não pode virar "0 mg").
     */
    public BigDecimal roundForDisplay(BigDecimal value) {
        Objects.requireNonNull(value, "value");
        boolean small = value.abs().compareTo(BigDecimal.TEN) < 0;
        int scale = (this == MILLIGRAM || this == MICROGRAM) && small ? 1 : displayScale;
        return value.setScale(scale, RoundingMode.HALF_UP);
    }
}
