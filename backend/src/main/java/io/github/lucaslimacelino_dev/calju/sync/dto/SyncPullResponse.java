package io.github.lucaslimacelino_dev.calju.sync.dto;

import java.util.List;

import io.github.lucaslimacelino_dev.calju.diary.dto.LogEntryResponse;
import io.github.lucaslimacelino_dev.calju.food.dto.FoodResponse;
import io.github.lucaslimacelino_dev.calju.goal.dto.GoalResponse;
import io.github.lucaslimacelino_dev.calju.meal.dto.MealResponse;

/**
 * O que mudou desde o cursor, inclusive exclusões ({@code deletedAt} preenchido). O app guarda
 * {@code cursor} e pede de novo enquanto {@code hasMore} for verdadeiro.
 */
public record SyncPullResponse(long cursor, boolean hasMore, List<GoalResponse> goals, List<FoodResponse> foods,
                               List<MealResponse> meals, List<LogEntryResponse> logEntries) {
}
