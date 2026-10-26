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
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.MealItemEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealWithItems;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.GoalRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.ItemDraft;
import io.github.lucaslimacelino_dev.calju.data.repository.MealRepository;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.PortionText;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

public class DiaryViewModel extends ViewModel {

    private static final int MAX_SUGGESTIONS = 8;
    private static final int MAX_OTHER_SUGGESTIONS = 30;

    private final CatalogRepository catalog;
    private final DiaryRepository diary;
    private final MealRepository meals;
    private final Executor executor;
    private final Clock clock;

    private final MutableLiveData<LocalDate> date;
    private final LiveData<DayState> dayState;
    private final MediatorLiveData<List<MealSummary>> mealSummaries = new MediatorLiveData<>();
    /** Aberta por padrão: as refeições salvas ficam à vista logo abaixo das do dia. */
    private final MutableLiveData<Boolean> mealsExpanded = new MutableLiveData<>(true);
    private final MutableLiveData<List<Suggestion>> suggestions = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<Integer> hiddenSuggestions = new MutableLiveData<>(0);
    private String lastSearch = "";
    private boolean showAllSuggestions;
    private final MutableLiveData<SelectedFood> selected = new MutableLiveData<>();
    /** Refeição escolhida pelo "+" de uma seção; nula = sugerir pelo horário. */
    private final MutableLiveData<Occasion> target = new MutableLiveData<>();
    private final MutableLiveData<DiaryEvent> events = new MutableLiveData<>();
    /** Cada busca recebe um número; resultados de buscas antigas são descartados. */
    private final AtomicInteger searchGeneration = new AtomicInteger();

    public DiaryViewModel(CatalogRepository catalog, DiaryRepository diary, GoalRepository goals,
                          MealRepository meals, Executor executor, Clock clock) {
        this.catalog = catalog;
        this.diary = diary;
        this.meals = meals;
        this.executor = executor;
        this.clock = clock;
        this.date = new MutableLiveData<>(LocalDate.now(clock));
        LiveData<DayEntries> entries = Transformations.switchMap(date, day ->
                Transformations.map(diary.observeDay(day), list -> new DayEntries(day, list)));
        this.dayState = combine(entries, goals.observeAll());
        // O resumo (nomes e kcal) precisa ler o catálogo, então é calculado fora da thread da tela.
        mealSummaries.addSource(meals.observeAll(), list ->
                executor.execute(() -> mealSummaries.postValue(summarize(list))));
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

    public LiveData<List<MealSummary>> mealSummaries() {
        return mealSummaries;
    }

    public LiveData<Boolean> mealsExpanded() {
        return mealsExpanded;
    }

    public LiveData<List<Suggestion>> suggestions() {
        return suggestions;
    }

    /** Quantas opções menos comuns combinam com o texto e estão escondidas ("Ver mais opções"). */
    public LiveData<Integer> hiddenSuggestions() {
        return hiddenSuggestions;
    }

    public LiveData<SelectedFood> selected() {
        return selected;
    }

    public LiveData<Occasion> target() {
        return target;
    }

    public void setTarget(Occasion occasion) {
        target.setValue(occasion);
    }

    public void clearTarget() {
        target.setValue(null);
    }

    public LiveData<DiaryEvent> events() {
        return events;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public void previousDay() {
        date.setValue(currentDate().minusDays(1));
    }

    /** Abre um dia específico (ex.: vindo do Histórico); dias futuros são ignorados. */
    public void showDate(LocalDate day) {
        if (!day.isAfter(today())) {
            date.setValue(day);
        }
    }

    public void nextDay() {
        if (currentDate().isBefore(today())) {
            date.setValue(currentDate().plusDays(1));
        }
    }

    public void toggleMeals() {
        mealsExpanded.setValue(!Boolean.TRUE.equals(mealsExpanded.getValue()));
    }

    public void onTextChanged(String text) {
        lastSearch = text;
        showAllSuggestions = false;
        runSearch(text);
    }

    public void showAllSuggestions() {
        showAllSuggestions = true;
        runSearch(lastSearch);
    }

    private void runSearch(String text) {
        int generation = searchGeneration.incrementAndGet();
        if (text.trim().length() < SuggestionSearch.MIN_QUERY_LENGTH) {
            suggestions.setValue(Collections.emptyList());
            hiddenSuggestions.setValue(0);
            return;
        }
        SuggestionSearch.Scope scope = showAllSuggestions ? SuggestionSearch.Scope.OTHERS
                : SuggestionSearch.Scope.FEATURED;
        int limit = showAllSuggestions ? MAX_OTHER_SUGGESTIONS : MAX_SUGGESTIONS;
        executor.execute(() -> {
            SuggestionSearch.Result result = SuggestionSearch.search(catalog, text, limit, scope);
            if (generation == searchGeneration.get()) {
                suggestions.postValue(result.items());
                hiddenSuggestions.postValue(result.hidden());
            }
        });
    }

    public void select(Suggestion suggestion) {
        Occasion occasion = occasionForNewEntry();
        executor.execute(() -> selected.postValue(new SelectedFood(suggestion.foodId(),
                catalog.food(suggestion.foodId()), suggestion.grams(), suggestion.portionText() != null, occasion,
                null, null)));
    }

    /** Abre a tela de quantidade para um alimento recém-criado, já com a porção dele, se houver. */
    public void selectFood(String foodId) {
        Occasion occasion = occasionForNewEntry();
        executor.execute(() -> {
            Food food = catalog.food(foodId);
            if (food != null) {
                BigDecimal grams = food.portion().isPresent() ? food.portion().get().grams() : null;
                selected.postValue(new SelectedFood(foodId, food, grams, grams != null, occasion, null, null));
            }
        });
    }

    /** Abre a tela de quantidade para um registro de um único item. */
    public void edit(EntryRow row) {
        Food food = row.snapshot();
        boolean units = food != null && food.portion().isPresent()
                && PortionText.count(row.totalGrams(), food.portion().get().grams()) != null;
        selected.setValue(new SelectedFood(null, food, row.totalGrams(), units, row.occasion(), row.itemId(),
                row.entryId()));
    }

    public void clearSelection() {
        selected.setValue(null);
    }

    public void clearSuggestions() {
        searchGeneration.incrementAndGet();
        suggestions.setValue(Collections.emptyList());
        hiddenSuggestions.setValue(0);
    }

    public void addFood(String foodId, BigDecimal grams, Occasion occasion) {
        LocalDate day = currentDate();
        executor.execute(() -> {
            String entryId = diary.addFood(day, occasion, foodId, grams);
            events.postValue(new DiaryEvent(DiaryEvent.Type.ADDED, entryId, occasion, null));
        });
    }

    /** "+" na lista: adiciona a refeição salva como está, na ocasião escolhida ou sugerida pelo horário. */
    public void addMealAsIs(String mealId) {
        Occasion occasion = occasionForNewEntry();
        LocalDate day = currentDate();
        executor.execute(() -> {
            MealWithItems meal = meals.byId(mealId);
            List<MealItemEntity> sorted = new ArrayList<>(meal.items);
            sorted.sort((a, b) -> Integer.compare(a.position, b.position));
            List<ItemDraft> items = new ArrayList<>();
            for (MealItemEntity item : sorted) {
                items.add(ItemDraft.of(item.foodId, item.quantityG));
            }
            String entryId = diary.addItems(day, occasion, meal.meal.name, mealId, items);
            events.postValue(new DiaryEvent(DiaryEvent.Type.ADDED, entryId, occasion, meal.meal.name));
        });
    }

    /** "Adicionar hoje" no editor: registra com os ajustes, sem alterar o molde. */
    public void addMealWithChanges(String mealId, String name, Occasion occasion, List<ItemDraft> items) {
        LocalDate day = currentDate();
        executor.execute(() -> {
            String entryId = diary.addItems(day, occasion, name, mealId, items);
            events.postValue(new DiaryEvent(DiaryEvent.Type.ADDED, entryId, occasion, name));
        });
    }

    /** Cria (mealId nulo) ou altera o molde de uma refeição salva. */
    public void saveMeal(String mealId, String name, List<ItemDraft> items) {
        executor.execute(() -> {
            String id;
            if (mealId == null) {
                id = meals.create(name, items);
            } else {
                meals.update(mealId, name, items);
                id = mealId;
            }
            events.postValue(new DiaryEvent(DiaryEvent.Type.MEAL_SAVED, id, null, name.trim()));
        });
    }

    public void duplicateMeal(String mealId, String copyName) {
        executor.execute(() -> {
            String copyId = meals.duplicate(mealId, copyName);
            events.postValue(new DiaryEvent(DiaryEvent.Type.MEAL_DUPLICATED, copyId, null, copyName));
        });
    }

    public void deleteMeal(String mealId, String name) {
        executor.execute(() -> {
            meals.delete(mealId);
            events.postValue(new DiaryEvent(DiaryEvent.Type.MEAL_DELETED, mealId, null, name));
        });
    }

    public void updateItem(String itemId, BigDecimal grams, Occasion occasion) {
        executor.execute(() -> diary.updateItem(itemId, grams, occasion));
    }

    public void updateEntry(String entryId, Occasion occasion, List<ItemDraft> items) {
        executor.execute(() -> diary.updateEntry(entryId, occasion, items));
    }

    public void removeEntry(String entryId, Occasion occasion) {
        executor.execute(() -> {
            diary.removeEntry(entryId);
            events.postValue(new DiaryEvent(DiaryEvent.Type.REMOVED, entryId, occasion, null));
        });
    }

    /** Desfazer: registro adicionado sai; registro removido ou refeição excluída voltam. */
    public void undo(DiaryEvent event) {
        executor.execute(() -> {
            switch (event.type()) {
                case ADDED:
                    diary.removeEntry(event.id());
                    break;
                case REMOVED:
                    diary.restoreEntry(event.id());
                    break;
                case MEAL_DELETED:
                    meals.restore(event.id());
                    break;
                default:
                    break;
            }
        });
    }

    private Occasion occasionForNewEntry() {
        Occasion chosen = target.getValue();
        return chosen != null ? chosen : Occasion.suggestFor(LocalTime.now(clock));
    }

    private LocalDate currentDate() {
        LocalDate current = date.getValue();
        return current != null ? current : today();
    }

    /** Nomes curtos ("Arroz, Feijão") e kcal do molde, com os valores atuais do catálogo. */
    private List<MealSummary> summarize(List<MealWithItems> list) {
        List<MealSummary> result = new ArrayList<>();
        for (MealWithItems meal : list) {
            List<MealItemEntity> sorted = new ArrayList<>(meal.items);
            sorted.sort((a, b) -> Integer.compare(a.position, b.position));
            List<Intake> intakes = new ArrayList<>();
            List<String> names = new ArrayList<>();
            for (MealItemEntity item : sorted) {
                Food food = catalog.food(item.foodId);
                if (food != null) {
                    intakes.add(new Intake(food, item.quantityG));
                    names.add(food.name().split(",")[0].trim());
                }
            }
            NutrientTotal kcal = NutritionCalculator.total(intakes, Nutrients.ENERGY);
            result.add(new MealSummary(meal.meal.id, meal.meal.name, String.join(", ", names),
                    kcal.complete() ? kcal.amount() : null));
        }
        return result;
    }

    public static ViewModelProvider.Factory factory(AppContainer container) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new DiaryViewModel(container.catalogRepository, container.diaryRepository,
                        container.goalRepository, container.mealRepository, container.databaseExecutor,
                        container.clock);
            }
        };
    }
}
