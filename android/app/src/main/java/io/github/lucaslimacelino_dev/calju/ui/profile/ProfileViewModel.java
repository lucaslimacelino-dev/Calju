package io.github.lucaslimacelino_dev.calju.ui.profile;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.math.BigDecimal;
import java.util.concurrent.Executor;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.data.repository.GoalRepository;
import io.github.lucaslimacelino_dev.calju.domain.Goal;

public class ProfileViewModel extends ViewModel {

    private final GoalRepository goals;
    private final Executor executor;
    private final LiveData<Goal> currentGoal;
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);

    public ProfileViewModel(GoalRepository goals, Executor executor) {
        this.goals = goals;
        this.executor = executor;
        this.currentGoal = Transformations.map(goals.observeAll(),
                list -> Goal.forDate(list, goals.today()).orElse(null));
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
                return (T) new ProfileViewModel(container.goalRepository, container.databaseExecutor);
            }
        };
    }
}
