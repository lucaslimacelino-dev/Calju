package io.github.lucaslimacelino_dev.calju.ui.diary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;

import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.data.local.NutrientEntity;
import io.github.lucaslimacelino_dev.calju.databinding.ItemNutrientRowBinding;
import io.github.lucaslimacelino_dev.calju.databinding.SheetMicronutrientsBinding;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

/** Todos os nutrientes do dia, sob demanda. */
public class MicronutrientsSheet extends BottomSheetDialogFragment {

    public static final String TAG = "MicronutrientsSheet";

    private SheetMicronutrientsBinding binding;
    private DiaryViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetMicronutrientsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(requireParentFragment(),
                DiaryViewModel.factory(((CaLjuApp) requireActivity().getApplication()).container()))
                .get(DiaryViewModel.class);
        viewModel.nutrients().observe(getViewLifecycleOwner(), nutrients -> render());
        viewModel.dayState().observe(getViewLifecycleOwner(), state -> render());
    }

    private void render() {
        List<NutrientEntity> nutrients = viewModel.nutrients().getValue();
        DayState state = viewModel.dayState().getValue();
        if (nutrients == null || state == null) {
            return;
        }
        binding.rows.removeAllViews();
        LayoutInflater inflater = getLayoutInflater();
        for (NutrientEntity nutrient : nutrients) {
            ItemNutrientRowBinding row = ItemNutrientRowBinding.inflate(inflater, binding.rows, true);
            row.name.setText(nutrient.namePt);
            row.value.setText(format(state.intakes(), nutrient));
        }
    }

    /** "—" se nenhum alimento tem o valor; "≥ X" se só alguns têm; senão o total. */
    private String format(List<Intake> intakes, NutrientEntity nutrient) {
        boolean anyKnown = intakes.isEmpty();
        for (Intake intake : intakes) {
            anyKnown |= intake.food().nutrient(nutrient.code).isPresent();
        }
        if (!anyKnown) {
            return getString(R.string.value_unknown);
        }
        NutrientTotal total = NutritionCalculator.total(intakes, nutrient.code);
        String value = Formats.amount(total.amount(), NutrientUnit.fromSymbol(nutrient.unit)) + " " + nutrient.unit;
        return total.complete() ? value : getString(R.string.value_at_least, value);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
