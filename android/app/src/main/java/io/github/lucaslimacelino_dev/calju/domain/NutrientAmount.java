package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Quantidade de um nutriente em 100 g (ou 100 ml) do alimento. */
public record NutrientAmount(BigDecimal amount, Qualifier qualifier) {

    public NutrientAmount {
        Objects.requireNonNull(qualifier, "qualifier");
        if (qualifier == Qualifier.TRACE) {
            amount = BigDecimal.ZERO;
        } else {
            Objects.requireNonNull(amount, "amount");
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("Quantidade de nutriente não pode ser negativa: " + amount);
            }
        }
    }

    public static NutrientAmount of(String amount) {
        return new NutrientAmount(new BigDecimal(amount), Qualifier.VALUE);
    }

    public static NutrientAmount trace() {
        return new NutrientAmount(BigDecimal.ZERO, Qualifier.TRACE);
    }
}
