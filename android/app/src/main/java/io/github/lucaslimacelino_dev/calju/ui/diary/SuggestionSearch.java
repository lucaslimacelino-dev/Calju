package io.github.lucaslimacelino_dev.calju.ui.diary;

import androidx.annotation.WorkerThread;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import io.github.lucaslimacelino_dev.calju.data.local.FoodSearchRow;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.FoodInput;
import io.github.lucaslimacelino_dev.calju.domain.FoodInputParser;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientAmount;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;
import io.github.lucaslimacelino_dev.calju.domain.Qualifier;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

/** Transforma o texto digitado em sugestões (usado no campo do Diário e no editor de refeições). */
final class SuggestionSearch {

    static final int MIN_QUERY_LENGTH = 2;

    private SuggestionSearch() {
    }

    @WorkerThread
    static List<Suggestion> search(CatalogRepository catalog, String text, int limit) {
        FoodInput input = FoodInputParser.parse(text);
        if (input.query().length() < MIN_QUERY_LENGTH) {
            return Collections.emptyList();
        }
        List<Suggestion> result = new ArrayList<>();
        for (FoodSearchRow row : catalog.search(input.query(), limit)) {
            Food food = catalog.food(row.id);
            if (food != null) {
                result.add(toSuggestion(row, food, input));
            }
        }
        return result;
    }

    private static Suggestion toSuggestion(FoodSearchRow row, Food food, FoodInput input) {
        Optional<BigDecimal> grams = input.gramsFor(food);
        BigDecimal kcal = grams.isPresent() ? knownKcal(food, grams.get()) : null;
        String portionText = null;
        if (grams.isPresent() && input.grams() == null && row.portionLabel != null) {
            BigDecimal units = input.units() != null ? input.units() : BigDecimal.ONE;
            portionText = units.compareTo(BigDecimal.ONE) == 0
                    ? row.portionLabel
                    : Formats.count(units) + " × " + row.portionLabel;
        }
        Optional<NutrientAmount> per100g = food.nutrient(Nutrients.ENERGY);
        BigDecimal kcalPer100g = per100g.isPresent() && per100g.get().qualifier() == Qualifier.VALUE
                ? per100g.get().amount() : null;
        String name = row.brand == null ? row.name : row.name + " · " + row.brand;
        return new Suggestion(row.id, name, grams.orElse(null), kcal, portionText, kcalPer100g);
    }

    private static BigDecimal knownKcal(Food food, BigDecimal grams) {
        NutrientTotal total = NutritionCalculator.total(
                Collections.singletonList(new Intake(food, grams)), Nutrients.ENERGY);
        return total.complete() ? total.amount() : null;
    }
}
