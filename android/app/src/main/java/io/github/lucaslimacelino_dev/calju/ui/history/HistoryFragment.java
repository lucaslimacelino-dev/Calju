package io.github.lucaslimacelino_dev.calju.ui.history;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.FragmentHistoryBinding;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.ui.DateRequest;
import io.github.lucaslimacelino_dev.calju.ui.Dates;
import io.github.lucaslimacelino_dev.calju.ui.Formats;
import io.github.lucaslimacelino_dev.calju.ui.history.HistoryState.WeekAverage;
import io.github.lucaslimacelino_dev.calju.ui.history.HistoryState.WeekBar;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;
    private LocalDate today;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        AppContainer container = ((CaLjuApp) requireActivity().getApplication()).container();
        HistoryViewModel viewModel = new ViewModelProvider(this, HistoryViewModel.factory(container))
                .get(HistoryViewModel.class);
        DateRequest dateRequest = new ViewModelProvider(requireActivity()).get(DateRequest.class);
        today = LocalDate.now(container.clock);

        HistoryAdapter adapter = new HistoryAdapter(today, date -> {
            dateRequest.open(date);
            requireActivity().<BottomNavigationView>findViewById(R.id.bottom_navigation)
                    .setSelectedItemId(R.id.diaryFragment);
        });
        binding.days.setAdapter(adapter);
        binding.previousWeek.setOnClickListener(v -> viewModel.previousWeek());
        binding.nextWeek.setOnClickListener(v -> viewModel.nextWeek());

        viewModel.state().observe(getViewLifecycleOwner(), state -> {
            showWeek(state);
            showAverage(state);
            showStreak(state);
            adapter.submitList(state.days());
            binding.emptyState.setVisibility(state.days().isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    private void showWeek(HistoryState state) {
        long weeksAgo = ChronoUnit.WEEKS.between(state.weekStart(), HistoryBuilder.weekStart(today));
        binding.weekTitle.setText(weeksAgo == 0 ? getString(R.string.history_this_week)
                : weeksAgo == 1 ? getString(R.string.history_last_week)
                : getResources().getQuantityString(R.plurals.history_weeks_ago, (int) weeksAgo, (int) weeksAgo));
        binding.weekRange.setText(Dates.range(requireContext(), state.weekStart(), state.weekStart().plusDays(6)));
        binding.previousWeek.setEnabled(state.hasPreviousWeek());
        binding.nextWeek.setEnabled(state.hasNextWeek());

        binding.weekChart.setData(state.bars(), state.goalKcal());
        binding.goalLegend.setText(state.goalKcal() == null ? getString(R.string.history_no_goal_legend)
                : getString(R.string.history_goal_legend, Formats.amount(state.goalKcal(), NutrientUnit.KCAL)));

        StringBuilder description = new StringBuilder();
        for (WeekBar bar : state.bars()) {
            if (bar.future()) {
                continue;
            }
            String day = Dates.title(requireContext(), bar.date(), today);
            description.append(' ').append(bar.kcal() == null ? getString(R.string.history_chart_day_empty, day)
                    : getString(R.string.history_chart_day, day, Formats.amount(bar.kcal(), NutrientUnit.KCAL)));
        }
        binding.weekChart.setContentDescription(getString(R.string.history_chart_description,
                description.toString().trim()));
    }

    private void showAverage(HistoryState state) {
        WeekAverage average = state.average();
        if (average == null) {
            binding.averageValue.setText(R.string.value_unknown);
            binding.averageMacros.setVisibility(View.GONE);
            binding.averageCaption.setText(R.string.history_no_average);
            return;
        }
        binding.averageValue.setText(getString(R.string.history_average_value,
                atLeast(average.kcal(), NutrientUnit.KCAL, average.complete())));
        String protein = atLeast(average.protein(), NutrientUnit.GRAM, average.complete());
        String carbohydrate = atLeast(average.carbohydrate(), NutrientUnit.GRAM, average.complete());
        String fat = atLeast(average.fat(), NutrientUnit.GRAM, average.complete());
        binding.averageMacros.setVisibility(View.VISIBLE);
        binding.averageMacros.setText(getString(R.string.history_average_macros, protein, carbohydrate, fat));
        binding.averageMacros.setContentDescription(getString(R.string.history_average_macros_description,
                protein, carbohydrate, fat));
        boolean includesToday = !today.isBefore(state.weekStart()) && !today.isAfter(state.weekStart().plusDays(6));
        binding.averageCaption.setText(getResources().getQuantityString(includesToday
                        ? R.plurals.history_average_days_today : R.plurals.history_average_days,
                average.days(), average.days()));
    }

    /** "≥ X" quando algum alimento não tinha o valor medido. */
    private String atLeast(BigDecimal value, NutrientUnit unit, boolean complete) {
        String text = Formats.amount(value, unit);
        return complete ? text : getString(R.string.value_at_least, text);
    }

    private void showStreak(HistoryState state) {
        if (state.streak() == 0) {
            binding.streakCaju.setImageResource(R.drawable.ic_caju_dormindo);
            binding.streakValue.setText(R.string.history_streak_none);
            binding.streakCaption.setText(R.string.history_streak_start);
            return;
        }
        binding.streakCaju.setImageResource(state.streakIncludesToday() ? R.drawable.ic_caju_comemorando
                : R.drawable.ic_caju_pensando);
        binding.streakValue.setText(getResources().getQuantityString(R.plurals.history_streak,
                state.streak(), state.streak()));
        binding.streakCaption.setText(state.streakIncludesToday() ? R.string.history_streak_today
                : R.string.history_streak_pending);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
