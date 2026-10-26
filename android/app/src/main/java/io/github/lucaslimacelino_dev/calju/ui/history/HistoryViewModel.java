package io.github.lucaslimacelino_dev.calju.ui.history;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.domain.Goal;

/** As últimas 12 semanas, recalculadas quando o diário, as metas ou a semana escolhida mudam. */
public class HistoryViewModel extends ViewModel {

    static final int WEEKS = 12;

    private final MediatorLiveData<HistoryState> state = new MediatorLiveData<>();
    /** 0 = esta semana, 1 = a anterior, e assim por diante. */
    private final MutableLiveData<Integer> weeksBack = new MutableLiveData<>(0);

    public HistoryViewModel(AppContainer container, Clock clock) {
        LocalDate today = LocalDate.now(clock);
        LocalDate thisWeek = HistoryBuilder.weekStart(today);
        LocalDate earliest = thisWeek.minusWeeks(WEEKS - 1);
        LiveData<List<EntryWithItems>> entries = container.diaryRepository.observeRange(earliest, today);
        LiveData<List<Goal>> goals = container.goalRepository.observeAll();
        Runnable rebuild = () -> {
            if (entries.getValue() != null && goals.getValue() != null) {
                LocalDate weekStart = thisWeek.minusWeeks(weeksBack.getValue());
                state.setValue(HistoryBuilder.build(today, weekStart, earliest, entries.getValue(), goals.getValue()));
            }
        };
        state.addSource(entries, value -> rebuild.run());
        state.addSource(goals, value -> rebuild.run());
        state.addSource(weeksBack, value -> rebuild.run());
    }

    public LiveData<HistoryState> state() {
        return state;
    }

    public void previousWeek() {
        int current = weeksBack.getValue();
        if (current < WEEKS - 1) {
            weeksBack.setValue(current + 1);
        }
    }

    public void nextWeek() {
        int current = weeksBack.getValue();
        if (current > 0) {
            weeksBack.setValue(current - 1);
        }
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
