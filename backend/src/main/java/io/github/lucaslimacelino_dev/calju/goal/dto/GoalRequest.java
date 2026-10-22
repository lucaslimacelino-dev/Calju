package io.github.lucaslimacelino_dev.calju.goal.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Corpo do PUT de uma meta. Macros são opcionais (nulo = sem meta para aquele macro). */
public record GoalRequest(
        @NotNull LocalDate effectiveFrom,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 4) BigDecimal kcal,
        @DecimalMin("0") @Digits(integer = 8, fraction = 4) BigDecimal proteinG,
        @DecimalMin("0") @Digits(integer = 8, fraction = 4) BigDecimal carbohydrateG,
        @DecimalMin("0") @Digits(integer = 8, fraction = 4) BigDecimal fatG,
        @NotNull @Min(0) Integer version) {
}
