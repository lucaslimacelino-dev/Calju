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

import java.time.LocalDate;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.FragmentHistoryBinding;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.ui.DateRequest;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

public class HistoryFragment extends Fragment {

    private FragmentHistoryBinding binding;

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

        HistoryAdapter adapter = new HistoryAdapter(LocalDate.now(container.clock), date -> {
            dateRequest.open(date);
            requireActivity().<BottomNavigationView>findViewById(R.id.bottom_navigation)
                    .setSelectedItemId(R.id.diaryFragment);
        });
        binding.days.setAdapter(adapter);

        viewModel.state().observe(getViewLifecycleOwner(), state -> {
            adapter.submitList(state.days());
            binding.emptyState.setVisibility(state.days().isEmpty() ? View.VISIBLE : View.GONE);
            if (state.weekAverage() == null) {
                binding.averageValue.setText(R.string.value_unknown);
                binding.averageCaption.setText(R.string.history_no_average);
            } else {
                String value = Formats.amount(state.weekAverage(), NutrientUnit.KCAL);
                binding.averageValue.setText(getString(R.string.history_average_value,
                        state.weekAverageComplete() ? value : getString(R.string.value_at_least, value)));
                binding.averageCaption.setText(getResources().getQuantityString(R.plurals.history_average_caption,
                        state.weekDaysWithRecords(), state.weekDaysWithRecords()));
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
