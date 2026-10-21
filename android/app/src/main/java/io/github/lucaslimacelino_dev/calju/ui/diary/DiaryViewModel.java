package io.github.lucaslimacelino_dev.calju.ui.diary;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.FoodSearchRow;
import io.github.lucaslimacelino_dev.calju.data.local.NutrientEntity;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.GoalRepository;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.FoodInput;
import io.github.lucaslimacelino_dev.calju.domain.FoodInputParser;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientAmount;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import io.github.lucaslimacelino_dev.calju.domain.Qualifier;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

public class DiaryViewModel extends ViewModel {

    private static final int MAX_SUGGESTIONS = 8;
    private static final int MIN_QUERY_LENGTH = 2;

    private final CatalogRepository catalog;
    private final DiaryRepository diary;
    private final Executor executor;
    private final Clock clock;

    private final MutableLiveData<LocalDate> date;
    private final LiveData<DayState> dayState;
    private final MutableLiveData<List<Suggestion>> suggestions = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<SelectedFood> selected = new MutableLiveData<>();
    private final MutableLiveData<DiaryEvent> events = new MutableLiveData<>();
    private final MutableLiveData<List<NutrientEntity>> nutrients = new MutableLiveData<>();
    /** Cada busca recebe um número; resultados de buscas antigas são descartados. */
    private final AtomicInteger searchGeneration = new AtomicInteger();

    public DiaryViewModel(CatalogRepository catalog, DiaryRepository diary, GoalRepository goals,
                          Executor executor, Clock clock) {
        this.catalog = catalog;
        this.diary = diary;
        this.executor = executor;
        this.clock = clock;
        this.date = new MutableLiveData<>(LocalDate.now(clock));
        LiveData<DayEntries> entries = Transformations.switchMap(date, day ->
                Transformations.map(diary.observeDay(day), list -> new DayEntries(day, list)));
        this.dayState = combine(entries, goals.observeAll());
        executor.execute(() -> nutrients.postValue(catalog.nutrients()));
    }

    /** Registros junto com a data a que pertencem (a data muda antes de os registros chegarem). */
    private record DayEntries(LocalDate date, List<EntryWithItems> entries) {
    }

    /**
     * Junta duas fontes que mudam independentemente: os registros do dia (que trocam quando a
     * data muda) e as metas. Qualquer mudança em uma delas recalcula o estado do dia.
     */
    private static LiveData<DayState> combine(LiveData<DayEntries> entries, LiveData<List<Goal>> goals) {
        MediatorLiveData<DayState> state = new MediatorLiveData<>();
        Runnable rebuild = () -> {
            DayEntries day = entries.getValue();
            if (day != null && goals.getValue() != null) {
                state.setValue(DayStateBuilder.build(day.date(), day.entries(), goals.getValue()));
            }
        };
        state.addSource(entries, value -> rebuild.run());
        state.addSource(goals, value -> rebuild.run());
        return state;
    }

    public LiveData<DayState> dayState() {
        return dayState;
    }

    public LiveData<LocalDate> date() {
        return date;
    }

    public LiveData<List<Suggestion>> suggestions() {
        return suggestions;
    }

    public LiveData<SelectedFood> selected() {
        return selected;
    }

    public LiveData<DiaryEvent> events() {
        return events;
    }

    public LiveData<List<NutrientEntity>> nutrients() {
        return nutrients;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public void previousDay() {
        date.setValue(currentDate().minusDays(1));
    }

    public void nextDay() {
        if (currentDate().isBefore(today())) {
            date.setValue(currentDate().plusDays(1));
        }
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
            for (FoodSearchRow row : catalog.search(input.query(), MAX_SUGGESTIONS)) {
                result.add(toSuggestion(row, catalog.food(row.id), input));
            }
            if (generation == searchGeneration.get()) {
                suggestions.postValue(result);
            }
        });
    }

    public void select(Suggestion suggestion) {
        Occasion occasion = Occasion.suggestFor(LocalTime.now(clock));
        executor.execute(() -> selected.postValue(new SelectedFood(suggestion.foodId(),
                catalog.food(suggestion.foodId()), suggestion.grams(), occasion, null, null)));
    }

    public void edit(EntryRow row) {
        selected.setValue(new SelectedFood(null, row.snapshot(), row.grams(), row.occasion(), row.itemId(),
                row.entryId()));
    }

    public void clearSelection() {
        selected.setValue(null);
    }

    public void clearSuggestions() {
        searchGeneration.incrementAndGet();
        suggestions.setValue(Collections.emptyList());
    }

    public void addFood(String foodId, BigDecimal grams, Occasion occasion) {
        LocalDate day = currentDate();
        executor.execute(() -> {
            String entryId = diary.addFood(day, occasion, foodId, grams);
            events.postValue(new DiaryEvent(DiaryEvent.Type.ADDED, entryId, occasion));
        });
    }

    public void updateItem(String itemId, BigDecimal grams, Occasion occasion) {
        executor.execute(() -> diary.updateItem(itemId, grams, occasion));
    }

    public void removeEntry(String entryId, Occasion occasion) {
        executor.execute(() -> {
            diary.removeEntry(entryId);
            events.postValue(new DiaryEvent(DiaryEvent.Type.REMOVED, entryId, occasion));
        });
    }

    /** Desfazer: um registro recém-adicionado é removido; um removido volta. */
    public void undo(DiaryEvent event) {
        executor.execute(() -> {
            if (event.type() == DiaryEvent.Type.ADDED) {
                diary.removeEntry(event.entryId());
            } else {
                diary.restoreEntry(event.entryId());
            }
        });
    }

    private LocalDate currentDate() {
        LocalDate current = date.getValue();
        return current != null ? current : today();
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
                return (T) new DiaryViewModel(container.catalogRepository, container.diaryRepository,
                        container.goalRepository, container.databaseExecutor, container.clock);
            }
        };
    }
}
