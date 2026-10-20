package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.ItemWithNutrients;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;

/** Monta o estado do dia a partir dos lançamentos gravados. Sem Android: testável no computador. */
public final class DayStateBuilder {

    private DayStateBuilder() {
    }

    public static DayState build(LocalDate date, List<EntryWithItems> entries) {
        List<Intake> intakes = new ArrayList<>();
        List<EntryRow> rows = new ArrayList<>();
        for (EntryWithItems entry : entries) {
            for (ItemWithNutrients item : entry.items) {
                Intake intake = DiaryRepository.toIntake(item);
                intakes.add(intake);
                NutrientTotal kcal = NutritionCalculator.total(Collections.singletonList(intake), Nutrients.ENERGY);
                rows.add(new EntryRow(entry.entry.id, item.item.id, entry.entry.occasion, entry.entry.createdAt,
                        item.item.foodNameSnap, item.item.quantityG, kcal.complete() ? kcal.amount() : null,
                        intake.food()));
            }
        }
        rows.sort(Comparator.comparing(EntryRow::occasion).thenComparingLong(EntryRow::createdAt));
        return new DayState(date,
                NutritionCalculator.total(intakes, Nutrients.ENERGY),
                NutritionCalculator.total(intakes, Nutrients.PROTEIN),
                NutritionCalculator.total(intakes, Nutrients.CARBOHYDRATE),
                NutritionCalculator.total(intakes, Nutrients.FAT),
                rows, intakes);
    }

    public static DayState empty(LocalDate date) {
        NutrientTotal zero = new NutrientTotal(BigDecimal.ZERO, true);
        return new DayState(date, zero, zero, zero, zero, Collections.emptyList(), Collections.emptyList());
    }
}
