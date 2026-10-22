package io.github.lucaslimacelino_dev.calju.ui.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.data.local.FoodEntity;
import io.github.lucaslimacelino_dev.calju.data.repository.AccountRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.GoalRepository;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.NutrientAmount;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;

public class ProfileViewModel extends ViewModel {

    /** Linha de "Meus alimentos"; {@code kcalPer100g} é nula se a energia não estiver gravada. */
    public record FoodSummary(String id, String name, String brand, BigDecimal kcalPer100g) {
    }

    private final GoalRepository goals;
    private final CatalogRepository catalog;
    private final AccountRepository account;
    private final Executor executor;
    private final Executor networkExecutor;
    private final LiveData<Goal> currentGoal;
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    private final MediatorLiveData<List<FoodSummary>> customFoods = new MediatorLiveData<>();

    public ProfileViewModel(GoalRepository goals, CatalogRepository catalog, AccountRepository account,
                            Executor executor, Executor networkExecutor) {
        this.goals = goals;
        this.catalog = catalog;
        this.account = account;
        this.executor = executor;
        this.networkExecutor = networkExecutor;
        this.currentGoal = Transformations.map(goals.observeAll(),
                list -> Goal.forDate(list, goals.today()).orElse(null));
        // As kcal ficam em outra tabela; a leitura é feita fora da thread da tela.
        customFoods.addSource(catalog.observeCustomFoods(), list ->
                executor.execute(() -> customFoods.postValue(summarize(list))));
    }

    public LiveData<List<FoodSummary>> customFoods() {
        return customFoods;
    }

    /** E-mail da conta conectada; nulo = sem conta. */
    public LiveData<String> signedInEmail() {
        return account.signedInEmail();
    }

    /** Sai da conta; os dados deste aparelho continuam. */
    public void signOut() {
        networkExecutor.execute(account::signOut);
    }

    public void restoreFood(String foodId) {
        executor.execute(() -> catalog.restoreCustomFood(foodId));
    }

    private List<FoodSummary> summarize(List<FoodEntity> list) {
        List<FoodSummary> result = new ArrayList<>();
        for (FoodEntity entity : list) {
            Food food = catalog.food(entity.id);
            Optional<NutrientAmount> kcal = food == null ? Optional.empty() : food.nutrient(Nutrients.ENERGY);
            result.add(new FoodSummary(entity.id, entity.name, entity.brand,
                    kcal.map(NutrientAmount::amount).orElse(null)));
        }
        return result;
    }

    /** Meta que vale hoje (nula se ainda não foi definida). */
    public LiveData<Goal> currentGoal() {
        return currentGoal;
    }

    public LiveData<Boolean> saved() {
        return saved;
    }

    public void save(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat) {
        Goal goal = new Goal(goals.today(), kcal, protein, carbohydrate, fat);
        executor.execute(() -> {
            goals.save(goal);
            saved.postValue(true);
        });
    }

    public void savedMessageShown() {
        saved.setValue(false);
    }

    public static ViewModelProvider.Factory factory(AppContainer container) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new ProfileViewModel(container.goalRepository, container.catalogRepository,
                        container.accountRepository, container.databaseExecutor, container.networkExecutor);
            }
        };
    }
}
