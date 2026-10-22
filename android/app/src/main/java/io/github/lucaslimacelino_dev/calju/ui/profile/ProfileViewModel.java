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
import io.github.lucaslimacelino_dev.calju.data.sync.AccountBinding;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncCoordinator;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncEngine;
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
    private final SyncEngine syncEngine;
    private final SyncCoordinator coordinator;
    private final MutableLiveData<AccountBinding.Decision> bindingDecision = new MutableLiveData<>();
    private final MutableLiveData<Integer> signOutPending = new MutableLiveData<>();
    private final MutableLiveData<SyncEngine.Result> syncResult = new MutableLiveData<>();
    private final MutableLiveData<Boolean> syncing = new MutableLiveData<>(false);
    private final Executor executor;
    private final Executor networkExecutor;
    private final LiveData<Goal> currentGoal;
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    private final MediatorLiveData<List<FoodSummary>> customFoods = new MediatorLiveData<>();

    public ProfileViewModel(GoalRepository goals, CatalogRepository catalog, AccountRepository account,
                            SyncEngine syncEngine, SyncCoordinator coordinator, Executor executor,
                            Executor networkExecutor) {
        this.goals = goals;
        this.catalog = catalog;
        this.account = account;
        this.syncEngine = syncEngine;
        this.coordinator = coordinator;
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

    public LiveData<SyncEngine.Result> syncResult() {
        return syncResult;
    }

    public LiveData<Boolean> syncing() {
        return syncing;
    }

    /** Sincroniza agora (envia o que mudou aqui e recebe o que mudou na conta). */
    public void syncNow() {
        if (Boolean.TRUE.equals(syncing.getValue())) {
            return;
        }
        syncing.setValue(true);
        networkExecutor.execute(() -> {
            syncResult.postValue(syncEngine.run());
            syncing.postValue(false);
        });
    }

    public void syncResultShown() {
        syncResult.setValue(null);
    }

    /** Pergunta a fazer logo depois de entrar (nula = nada a perguntar). */
    public LiveData<AccountBinding.Decision> bindingDecision() {
        return bindingDecision;
    }

    public String boundEmail() {
        return coordinator.boundEmail();
    }

    public void afterSignIn(String email) {
        networkExecutor.execute(() -> {
            AccountBinding.Decision decision = coordinator.afterSignIn(email);
            bindingDecision.postValue(decision == AccountBinding.Decision.CONTINUE ? null : decision);
        });
    }

    public void uploadLocalData(String email) {
        bindingDecision.setValue(null);
        networkExecutor.execute(() -> coordinator.uploadLocalData(email));
    }

    public void replaceLocalData(String email) {
        bindingDecision.setValue(null);
        networkExecutor.execute(() -> coordinator.replaceLocalData(email));
    }

    public void cancelSignIn() {
        bindingDecision.setValue(null);
        networkExecutor.execute(coordinator::cancelSignIn);
    }

    /** Quantas mudanças ainda não foram enviadas, para a pergunta de "Sair". */
    public LiveData<Integer> signOutPending() {
        return signOutPending;
    }

    public void prepareSignOut() {
        executor.execute(() -> signOutPending.postValue(coordinator.pendingCount()));
    }

    /** Sai da conta, mantendo ou apagando os dados deste aparelho. */
    public void signOut(boolean keepData) {
        signOutPending.setValue(null);
        networkExecutor.execute(() -> coordinator.signOut(keepData));
    }

    public void signOutCancelled() {
        signOutPending.setValue(null);
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
                        container.accountRepository, container.syncEngine, container.syncCoordinator,
                        container.databaseExecutor, container.networkExecutor);
            }
        };
    }
}
