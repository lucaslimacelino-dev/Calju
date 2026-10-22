package io.github.lucaslimacelino_dev.calju.meal.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

/** Item na ordem em que vem na lista; a posição é o índice. */
public record MealItemDto(
        @NotNull UUID id,
        @NotNull UUID foodId,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 4) BigDecimal quantityG) {
}
