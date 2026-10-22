package io.github.lucaslimacelino_dev.calju.meal.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.meal.MealEntity;

public record MealResponse(UUID id, String name, List<MealItemDto> items, int version, Instant updatedAt,
                           Instant deletedAt) {

    public static MealResponse from(MealEntity meal) {
        List<MealItemDto> items = meal.getItems().stream()
                .map(item -> new MealItemDto(item.getId(), item.getFoodId(), item.getQuantityG()))
                .toList();
        return new MealResponse(meal.getId(), meal.getName(), items, meal.getVersion(), meal.getUpdatedAt(),
                meal.getDeletedAt());
    }
}
