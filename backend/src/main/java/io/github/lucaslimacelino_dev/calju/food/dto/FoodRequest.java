package io.github.lucaslimacelino_dev.calju.food.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo do PUT de um alimento do usuário. {@code nutrients} é a lista completa: o que não vier
 * fica desconhecido. Valores por 100 g, como o app grava.
 */
public record FoodRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String brand,
        @NotNull @Size(max = 100) List<@Valid @NotNull NutrientValueDto> nutrients,
        @Valid PortionDto portion,
        @NotNull @Min(0) Integer version) {
}
