package io.github.lucaslimacelino_dev.calju.catalog;

import java.math.BigDecimal;

/** Valor por 100 g. Em um traço, {@code amount} é nulo e {@code trace} é verdadeiro. */
public record NutrientValue(String nutrientCode, BigDecimal amount, boolean trace) {
}
