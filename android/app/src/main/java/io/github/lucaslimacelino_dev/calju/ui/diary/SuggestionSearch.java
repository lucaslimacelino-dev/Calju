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
    private static final int SEARCH_LIMIT = 300;

    /** Quais alimentos mostrar. */
    enum Scope {
        /** Comuns e criados pelo usuário (ou todos, se nenhum desses combinar com o texto). */
        FEATURED,
        /** Só os que ficaram de fora do FEATURED ("Ver mais opções"). */
        OTHERS,
        /** Todos, com os comuns primeiro. */
        ALL
    }

    /** Sugestões e quantas outras opções ficaram para "Ver mais opções". */
    record Result(List<Suggestion> items, int hidden) {

        static final Result EMPTY = new Result(Collections.emptyList(), 0);
    }

    private SuggestionSearch() {
    }

    /**
     * Em FEATURED, os demais alimentos da TACO (crus, cortes raros) ficam escondidos, a não ser que
     * nenhum comum combine com o texto; OTHERS mostra justamente esses escondidos.
     */
    @WorkerThread
    static Result search(CatalogRepository catalog, String text, int limit, Scope scope) {
        FoodInput input = FoodInputParser.parse(text);
        if (input.query().length() < MIN_QUERY_LENGTH) {
            return Result.EMPTY;
        }
        List<FoodSearchRow> rows = catalog.search(input.query(), SEARCH_LIMIT);
        List<FoodSearchRow> featured = new ArrayList<>();
        List<FoodSearchRow> others = new ArrayList<>();
        for (FoodSearchRow row : rows) {
            (row.featured() ? featured : others).add(row);
        }
        boolean onlyFeatured = scope == Scope.FEATURED && !featured.isEmpty();
        List<FoodSearchRow> shown = scope == Scope.OTHERS ? others : onlyFeatured ? featured : rows;
        List<Suggestion> result = new ArrayList<>();
        for (FoodSearchRow row : shown.subList(0, Math.min(limit, shown.size()))) {
            Food food = catalog.food(row.id);
            if (food != null) {
                result.add(toSuggestion(row, food, input));
            }
        }
        return new Result(result, onlyFeatured ? others.size() : 0);
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
        String name = row.brand == null ? row.shownName() : row.shownName() + " · " + row.brand;
        return new Suggestion(row.id, name, grams.orElse(null), kcal, portionText, kcalPer100g, food.unit());
    }

    private static BigDecimal knownKcal(Food food, BigDecimal grams) {
        NutrientTotal total = NutritionCalculator.total(
                Collections.singletonList(new Intake(food, grams)), Nutrients.ENERGY);
        return total.complete() ? total.amount() : null;
    }
}
