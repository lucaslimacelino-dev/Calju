package io.github.lucaslimacelino_dev.calju.ui.diary;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.ItemWithNutrients;
import io.github.lucaslimacelino_dev.calju.data.local.MealItemEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealWithItems;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.ItemDraft;
import io.github.lucaslimacelino_dev.calju.data.repository.MealRepository;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

/** Estado do editor de refeição: itens, gramas digitadas, total e busca para incluir alimentos. */
public class MealEditorViewModel extends ViewModel {

    public enum Mode { NEW_MEAL, EDIT_MEAL, ENTRY }

    private static final int MAX_SUGGESTIONS = 6;

    private final CatalogRepository catalog;
    private final MealRepository meals;
    private final DiaryRepository diary;
    private final Executor executor;
    private final Clock clock;

    private final MutableLiveData<List<EditorItem>> items = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> name = new MutableLiveData<>();
    private final MutableLiveData<Occasion> occasion = new MutableLiveData<>();
    private final MutableLiveData<NutrientTotal> total = new MutableLiveData<>();
    private final MutableLiveData<List<Suggestion>> suggestions = new MutableLiveData<>(Collections.emptyList());
    private final AtomicInteger searchGeneration = new AtomicInteger();
    private final Handler mainThread = new Handler(Looper.getMainLooper());
    private boolean loaded;

    public MealEditorViewModel(CatalogRepository catalog, MealRepository meals, DiaryRepository diary,
                               Executor executor, Clock clock) {
        this.catalog = catalog;
        this.meals = meals;
        this.diary = diary;
        this.executor = executor;
        this.clock = clock;
    }

    public LiveData<List<EditorItem>> items() {
        return items;
    }

    public LiveData<String> name() {
        return name;
    }

    public LiveData<Occasion> occasion() {
        return occasion;
    }

    public LiveData<NutrientTotal> total() {
        return total;
    }

    public LiveData<List<Suggestion>> suggestions() {
        return suggestions;
    }

    /** Carrega a refeição ou o registro uma única vez (sobrevive à rotação da tela). */
    public void load(Mode mode, String id) {
        if (loaded) {
            return;
        }
        loaded = true;
        Occasion suggested = Occasion.suggestFor(LocalTime.now(clock));
        executor.execute(() -> {
            List<EditorItem> loadedItems = new ArrayList<>();
            String loadedName = "";
            Occasion loadedOccasion = suggested;
            if (mode == Mode.EDIT_MEAL) {
                MealWithItems meal = meals.byId(id);
                loadedName = meal.meal.name;
                List<MealItemEntity> mealItems = new ArrayList<>(meal.items);
                mealItems.sort((a, b) -> Integer.compare(a.position, b.position));
                for (MealItemEntity item : mealItems) {
                    Food food = catalog.food(item.foodId);
                    if (food != null) {
                        loadedItems.add(new EditorItem(null, item.foodId, food, text(item.quantityG)));
                    }
                }
            } else if (mode == Mode.ENTRY) {
                EntryWithItems entry = diary.entry(id);
                loadedOccasion = entry.entry.occasion;
                List<ItemWithNutrients> entryItems = new ArrayList<>(entry.items);
                entryItems.sort((a, b) -> Integer.compare(a.item.position, b.item.position));
                loadedName = entry.entry.title != null || entryItems.isEmpty()
                        ? entry.entry.title : entryItems.get(0).item.foodNameSnap;
                for (ItemWithNutrients item : entryItems) {
                    Intake intake = DiaryRepository.toIntake(item);
                    loadedItems.add(new EditorItem(item.item.id, item.item.foodId, intake.food(),
                            text(item.item.quantityG)));
                }
            }
            name.postValue(loadedName);
            occasion.postValue(loadedOccasion);
            items.postValue(loadedItems);
            total.postValue(computeTotal(loadedItems));
        });
    }

    public void search(String text) {
        int generation = searchGeneration.incrementAndGet();
        if (text.trim().length() < SuggestionSearch.MIN_QUERY_LENGTH) {
            suggestions.setValue(Collections.emptyList());
            return;
        }
        executor.execute(() -> {
            List<Suggestion> result = SuggestionSearch.search(catalog, text, MAX_SUGGESTIONS, SuggestionSearch.Scope.ALL).items();
            if (generation == searchGeneration.get()) {
                suggestions.postValue(result);
            }
        });
    }

    public void include(Suggestion suggestion) {
        searchGeneration.incrementAndGet();
        suggestions.setValue(Collections.emptyList());
        executor.execute(() -> {
            Food food = catalog.food(suggestion.foodId());
            String grams = suggestion.grams() == null ? "" : text(suggestion.grams());
            // Só a busca no banco roda em segundo plano; a lista é alterada na thread da tela.
            mainThread.post(() -> {
                List<EditorItem> updated = new ArrayList<>(current());
                updated.add(new EditorItem(null, suggestion.foodId(), food, grams));
                items.setValue(updated);
                total.setValue(computeTotal(updated));
            });
        });
    }

    public void remove(String key) {
        List<EditorItem> updated = new ArrayList<>();
        for (EditorItem item : current()) {
            if (!item.key.equals(key)) {
                updated.add(item);
            }
        }
        items.setValue(updated);
        total.setValue(computeTotal(updated));
    }

    /** Atualiza só o texto digitado; a lista não é redesenhada para não atrapalhar a digitação. */
    public void gramsChanged(String key, String text) {
        for (EditorItem item : current()) {
            if (item.key.equals(key)) {
                item.gramsText = text;
            }
        }
        total.setValue(computeTotal(current()));
    }

    /** Itens prontos para gravar, ou nulo se alguma quantidade for inválida. */
    public List<ItemDraft> drafts() {
        List<ItemDraft> drafts = new ArrayList<>();
        for (EditorItem item : current()) {
            BigDecimal grams = Formats.parsePositive(item.gramsText);
            if (grams == null) {
                return null;
            }
            drafts.add(new ItemDraft(item.itemId, item.foodId, grams));
        }
        return drafts;
    }

    private List<EditorItem> current() {
        List<EditorItem> value = items.getValue();
        return value != null ? value : Collections.emptyList();
    }

    /** Itens com quantidade inválida deixam o total incompleto ("≥"). */
    private static NutrientTotal computeTotal(List<EditorItem> list) {
        List<Intake> intakes = new ArrayList<>();
        boolean allValid = true;
        for (EditorItem item : list) {
            BigDecimal grams = Formats.parsePositive(item.gramsText);
            if (grams == null) {
                allValid = false;
            } else {
                intakes.add(new Intake(item.food, grams));
            }
        }
        NutrientTotal total = NutritionCalculator.total(intakes, Nutrients.ENERGY);
        return new NutrientTotal(total.amount(), total.complete() && allValid);
    }

    private static String text(BigDecimal grams) {
        return grams.stripTrailingZeros().toPlainString().replace('.', ',');
    }

    public static ViewModelProvider.Factory factory(AppContainer container) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new MealEditorViewModel(container.catalogRepository, container.mealRepository,
                        container.diaryRepository, container.databaseExecutor, container.clock);
            }
        };
    }
}
