package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;

import io.github.lucaslimacelino_dev.calju.domain.Food;

/** Alimento escolhido na lista, com as gramas sugeridas (nulas quando é preciso perguntar). */
public record SelectedFood(String foodId, Food food, BigDecimal suggestedGrams) {
}
