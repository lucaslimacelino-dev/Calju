package io.github.lucaslimacelino_dev.calju.food.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Corpo do PUT de um alimento do usuário. {@code nutrients} é a lista completa: o que não vier
 * fica desconhecido. Valores por 100 g (ou 100 ml), como o app grava. {@code unit} ausente = "g".
 */
public record FoodRequest(
        @NotBlank @Size(max = 200) String name,
        @Size(max = 200) String brand,
        @Pattern(regexp = "g|ml") String unit,
        @NotNull @Size(max = 100) List<@Valid @NotNull NutrientValueDto> nutrients,
        @Valid PortionDto portion,
        @NotNull @Min(0) Integer version) {
}
