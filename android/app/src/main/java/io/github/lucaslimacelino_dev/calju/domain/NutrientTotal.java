package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Soma de um nutriente. Quando {@code complete} é falso, algum item não tem o valor
 * conhecido e o total real é maior ou igual a {@code amount}.
 */
public record NutrientTotal(BigDecimal amount, boolean complete) {

    public NutrientTotal {
        Objects.requireNonNull(amount, "amount");
    }
}
