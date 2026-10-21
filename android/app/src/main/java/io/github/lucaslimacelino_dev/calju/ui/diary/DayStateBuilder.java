package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.ItemWithNutrients;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;

/** Monta o estado do dia a partir dos lançamentos gravados. Sem Android: testável no computador. */
public final class DayStateBuilder {

    private DayStateBuilder() {
    }

    public static DayState build(LocalDate date, List<EntryWithItems> entries, List<Goal> goals) {
        List<Intake> intakes = new ArrayList<>();
        List<EntryRow> rows = new ArrayList<>();
        for (EntryWithItems entry : entries) {
            if (entry.items.isEmpty()) {
                continue;
            }
            List<ItemWithNutrients> items = new ArrayList<>(entry.items);
            items.sort(Comparator.comparingInt(item -> item.item.position));
            List<Intake> entryIntakes = new ArrayList<>();
            BigDecimal grams = BigDecimal.ZERO;
            for (ItemWithNutrients item : items) {
                Intake intake = DiaryRepository.toIntake(item);
                entryIntakes.add(intake);
                grams = grams.add(intake.grams());
            }
            intakes.addAll(entryIntakes);
            NutrientTotal kcal = NutritionCalculator.total(entryIntakes, Nutrients.ENERGY);
            boolean single = items.size() == 1;
            String title = entry.entry.title != null ? entry.entry.title : items.get(0).item.foodNameSnap;
            rows.add(new EntryRow(entry.entry.id, entry.entry.occasion, entry.entry.createdAt, title, items.size(),
                    grams, kcal.complete() ? kcal.amount() : null,
                    single ? items.get(0).item.id : null, single ? entryIntakes.get(0).food() : null));
        }
        rows.sort(Comparator.comparing(EntryRow::occasion).thenComparingLong(EntryRow::createdAt));
        return new DayState(date,
                NutritionCalculator.total(intakes, Nutrients.ENERGY),
                NutritionCalculator.total(intakes, Nutrients.PROTEIN),
                NutritionCalculator.total(intakes, Nutrients.CARBOHYDRATE),
                NutritionCalculator.total(intakes, Nutrients.FAT),
                rows, intakes, Goal.forDate(goals, date).orElse(null));
    }
}
