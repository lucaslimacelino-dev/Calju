package io.github.lucaslimacelino_dev.calju.ui.diary;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.lucaslimacelino_dev.calju.AppContainer;
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

public class DiaryViewModel extends ViewModel {

    private static final int MAX_SUGGESTIONS = 8;
    private static final int MIN_QUERY_LENGTH = 2;

    private final CatalogRepository repository;
    private final Executor executor;
    private final MutableLiveData<List<Suggestion>> suggestions = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<SelectedFood> selected = new MutableLiveData<>();
    /** Cada busca recebe um número; resultados de buscas antigas são descartados. */
    private final AtomicInteger searchGeneration = new AtomicInteger();

    public DiaryViewModel(CatalogRepository repository, Executor executor) {
        this.repository = repository;
        this.executor = executor;
    }

    public LiveData<List<Suggestion>> suggestions() {
        return suggestions;
    }

    public LiveData<SelectedFood> selected() {
        return selected;
    }

    public void onTextChanged(String text) {
        int generation = searchGeneration.incrementAndGet();
        FoodInput input = FoodInputParser.parse(text);
        if (input.query().length() < MIN_QUERY_LENGTH) {
            suggestions.setValue(Collections.emptyList());
            return;
        }
        executor.execute(() -> {
            List<Suggestion> result = new ArrayList<>();
            for (FoodSearchRow row : repository.search(input.query(), MAX_SUGGESTIONS)) {
                result.add(toSuggestion(row, repository.food(row.id), input));
            }
            if (generation == searchGeneration.get()) {
                suggestions.postValue(result);
            }
        });
    }

    public void select(Suggestion suggestion) {
        executor.execute(() -> selected.postValue(
                new SelectedFood(suggestion.foodId(), repository.food(suggestion.foodId()), suggestion.grams())));
    }

    public void clearSelection() {
        selected.setValue(null);
    }

    public void clearSuggestions() {
        searchGeneration.incrementAndGet();
        suggestions.setValue(Collections.emptyList());
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
        return new Suggestion(row.id, row.name, grams.orElse(null), kcal, portionText, kcalPer100g);
    }

    private static BigDecimal knownKcal(Food food, BigDecimal grams) {
        NutrientTotal total = NutritionCalculator.total(
                Collections.singletonList(new Intake(food, grams)), Nutrients.ENERGY);
        return total.complete() ? total.amount() : null;
    }

    public static ViewModelProvider.Factory factory(AppContainer container) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new DiaryViewModel(container.catalogRepository, container.databaseExecutor);
            }
        };
    }
}
