package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.time.LocalDate;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;

/** Tudo o que o Diário mostra para um dia. {@code goal} é nulo se não houver meta vigente. */
public record DayState(LocalDate date, NutrientTotal kcal, NutrientTotal protein, NutrientTotal carbohydrate,
                       NutrientTotal fat, List<EntryRow> entries, List<Intake> intakes, Goal goal) {
}
