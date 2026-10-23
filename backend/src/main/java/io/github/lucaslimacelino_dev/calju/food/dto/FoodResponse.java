package io.github.lucaslimacelino_dev.calju.food.dto;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.food.FoodEntity;
import io.github.lucaslimacelino_dev.calju.food.FoodPortion;

/** {@code deletedAt} preenchido = alimento excluído (só aparece ao buscar pelo id). */
public record FoodResponse(UUID id, String name, String brand, String unit, List<NutrientValueDto> nutrients,
                           PortionDto portion, int version, Instant updatedAt, Instant deletedAt) {

    public static FoodResponse from(FoodEntity food) {
        List<NutrientValueDto> nutrients = food.getNutrients().stream()
                .map(NutrientValueDto::from)
                .sorted(Comparator.comparing(NutrientValueDto::code))
                .toList();
        FoodPortion portion = food.getPortion();
        return new FoodResponse(food.getId(), food.getName(), food.getBrand(), food.getUnit(), nutrients,
                portion == null ? null : new PortionDto(portion.getLabel(), portion.getGrams()),
                food.getVersion(), food.getUpdatedAt(), food.getDeletedAt());
    }
}
