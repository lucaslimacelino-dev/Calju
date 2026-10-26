package io.github.lucaslimacelino_dev.calju.ui.history;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.ItemWithNutrients;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;
import io.github.lucaslimacelino_dev.calju.ui.history.HistoryState.WeekAverage;
import io.github.lucaslimacelino_dev.calju.ui.history.HistoryState.WeekBar;

/** Monta a semana do histórico a partir dos registros. Sem Android: testável no computador. */
public final class HistoryBuilder {

    static final int WEEK_DAYS = 7;

    private HistoryBuilder() {
    }

    /** Segunda-feira da semana que contém a data. */
    public static LocalDate weekStart(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * @param weekStart         segunda-feira da semana mostrada
     * @param earliestWeekStart semana mais antiga carregada (limite do botão "semana anterior")
     */
    public static HistoryState build(LocalDate today, LocalDate weekStart, LocalDate earliestWeekStart,
                                     List<EntryWithItems> entries, List<Goal> goals) {
        Map<LocalDate, HistoryDay> byDay = totals(entries, goals);

        List<WeekBar> bars = new ArrayList<>();
        List<HistoryDay> days = new ArrayList<>();
        BigDecimal kcal = BigDecimal.ZERO;
        BigDecimal protein = BigDecimal.ZERO;
        BigDecimal carbohydrate = BigDecimal.ZERO;
        BigDecimal fat = BigDecimal.ZERO;
        int counted = 0;
        boolean complete = true;
        for (int i = 0; i < WEEK_DAYS; i++) {
            LocalDate date = weekStart.plusDays(i);
            HistoryDay day = byDay.get(date);
            bars.add(new WeekBar(date, day == null ? null : day.kcal().amount(), date.equals(today),
                    date.isAfter(today)));
            if (day == null) {
                continue;
            }
            days.add(0, day);
            if (date.isBefore(today)) {
                kcal = kcal.add(day.kcal().amount());
                protein = protein.add(day.protein().amount());
                carbohydrate = carbohydrate.add(day.carbohydrate().amount());
                fat = fat.add(day.fat().amount());
                complete &= day.kcal().complete() && day.protein().complete()
                        && day.carbohydrate().complete() && day.fat().complete();
                counted++;
            }
        }
        WeekAverage average = counted == 0 ? null
                : new WeekAverage(divide(kcal, counted), divide(protein, counted), divide(carbohydrate, counted),
                divide(fat, counted), counted, complete);

        LocalDate weekEnd = weekStart.plusDays(WEEK_DAYS - 1);
        LocalDate lastLived = weekEnd.isAfter(today) ? today : weekEnd;
        BigDecimal goalKcal = Goal.forDate(goals, lastLived).map(Goal::kcal).orElse(null);

        boolean streakIncludesToday = byDay.containsKey(today);
        int streak = 0;
        LocalDate day = streakIncludesToday ? today : today.minusDays(1);
        while (byDay.containsKey(day)) {
            streak++;
            day = day.minusDays(1);
        }

        return new HistoryState(weekStart, bars, goalKcal, average, streak, streakIncludesToday, days,
                weekStart.isAfter(earliestWeekStart), weekStart.isBefore(weekStart(today)));
    }

    private static Map<LocalDate, HistoryDay> totals(List<EntryWithItems> entries, List<Goal> goals) {
        Map<LocalDate, List<Intake>> intakesByDay = new HashMap<>();
        for (EntryWithItems entry : entries) {
            List<Intake> intakes = intakesByDay.computeIfAbsent(entry.entry.logDate, date -> new ArrayList<>());
            for (ItemWithNutrients item : entry.items) {
                intakes.add(DiaryRepository.toIntake(item));
            }
        }
        Map<LocalDate, HistoryDay> byDay = new HashMap<>();
        for (Map.Entry<LocalDate, List<Intake>> day : intakesByDay.entrySet()) {
            List<Intake> intakes = day.getValue();
            if (intakes.isEmpty()) {
                continue;
            }
            byDay.put(day.getKey(), new HistoryDay(day.getKey(),
                    NutritionCalculator.total(intakes, Nutrients.ENERGY),
                    NutritionCalculator.total(intakes, Nutrients.PROTEIN),
                    NutritionCalculator.total(intakes, Nutrients.CARBOHYDRATE),
                    NutritionCalculator.total(intakes, Nutrients.FAT),
                    Goal.forDate(goals, day.getKey()).orElse(null)));
        }
        return byDay;
    }

    private static BigDecimal divide(BigDecimal sum, int days) {
        return sum.divide(BigDecimal.valueOf(days), 4, RoundingMode.HALF_UP);
    }
}
