package io.github.lucaslimacelino_dev.calju.meal.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corpo do PUT de uma refeição salva: nome e a lista completa de itens (ao menos um). */
public record MealRequest(
        @NotBlank @Size(max = 200) String name,
        @NotNull @Size(min = 1, max = 50) List<@Valid @NotNull MealItemDto> items,
        @NotNull @Min(0) Integer version) {
}
