package io.github.lucaslimacelino_dev.calju.ui.history;

import java.time.LocalDate;

import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;

/** Um dia com registros: totais e a meta que valia naquele dia (nula se não havia). */
public record HistoryDay(LocalDate date, NutrientTotal kcal, NutrientTotal protein, NutrientTotal carbohydrate,
                         NutrientTotal fat, Goal goal) {
}
