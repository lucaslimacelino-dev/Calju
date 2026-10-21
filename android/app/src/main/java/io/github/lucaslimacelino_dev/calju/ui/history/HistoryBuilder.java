package io.github.lucaslimacelino_dev.calju.ui.history;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.ItemWithNutrients;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;

/** Monta o histórico a partir dos registros. Sem Android: testável no computador. */
public final class HistoryBuilder {

    static final int AVERAGE_DAYS = 7;

    private HistoryBuilder() {
    }

    public static HistoryState build(LocalDate today, List<EntryWithItems> entries, List<Goal> goals) {
        Map<LocalDate, List<Intake>> byDay = new TreeMap<>((a, b) -> b.compareTo(a));
        for (EntryWithItems entry : entries) {
            List<Intake> intakes = byDay.computeIfAbsent(entry.entry.logDate, day -> new ArrayList<>());
            for (ItemWithNutrients item : entry.items) {
                intakes.add(DiaryRepository.toIntake(item));
            }
        }

        List<HistoryDay> days = new ArrayList<>();
        BigDecimal weekSum = BigDecimal.ZERO;
        int weekDays = 0;
        boolean weekComplete = true;
        LocalDate weekStart = today.minusDays(AVERAGE_DAYS);
        for (Map.Entry<LocalDate, List<Intake>> day : byDay.entrySet()) {
            List<Intake> intakes = day.getValue();
            if (intakes.isEmpty()) {
                continue;
            }
            NutrientTotal kcal = NutritionCalculator.total(intakes, Nutrients.ENERGY);
            days.add(new HistoryDay(day.getKey(), kcal,
                    NutritionCalculator.total(intakes, Nutrients.PROTEIN),
                    NutritionCalculator.total(intakes, Nutrients.CARBOHYDRATE),
                    NutritionCalculator.total(intakes, Nutrients.FAT),
                    Goal.forDate(goals, day.getKey()).orElse(null)));
            boolean inWeek = day.getKey().isBefore(today) && !day.getKey().isBefore(weekStart);
            if (inWeek) {
                weekSum = weekSum.add(kcal.amount());
                weekDays++;
                weekComplete &= kcal.complete();
            }
        }
        BigDecimal average = weekDays == 0 ? null
                : weekSum.divide(BigDecimal.valueOf(weekDays), 4, RoundingMode.HALF_UP);
        return new HistoryState(days, average, weekDays, weekComplete);
    }
}
