package io.github.lucaslimacelino_dev.calju.ui.history;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.domain.Goal;

/** Últimos 60 dias com registro, recalculados quando o diário ou as metas mudam. */
public class HistoryViewModel extends ViewModel {

    static final int DAYS = 60;

    private final MediatorLiveData<HistoryState> state = new MediatorLiveData<>();

    public HistoryViewModel(AppContainer container, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        LiveData<List<EntryWithItems>> entries =
                container.diaryRepository.observeRange(today.minusDays(DAYS - 1), today);
        LiveData<List<Goal>> goals = container.goalRepository.observeAll();
        Runnable rebuild = () -> {
            if (entries.getValue() != null && goals.getValue() != null) {
                state.setValue(HistoryBuilder.build(today, entries.getValue(), goals.getValue()));
            }
        };
        state.addSource(entries, value -> rebuild.run());
        state.addSource(goals, value -> rebuild.run());
    }

    public LiveData<HistoryState> state() {
        return state;
    }

    public static ViewModelProvider.Factory factory(AppContainer container) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new HistoryViewModel(container, container.clock);
            }
        };
    }
}
