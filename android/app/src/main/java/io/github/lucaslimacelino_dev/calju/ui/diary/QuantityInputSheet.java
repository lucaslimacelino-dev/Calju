package io.github.lucaslimacelino_dev.calju.ui.diary;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.math.BigDecimal;
import java.util.Collections;

import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.SheetQuantityBinding;
import io.github.lucaslimacelino_dev.calju.databinding.ViewNutrientValueBinding;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;
import io.github.lucaslimacelino_dev.calju.domain.Portion;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

/** Painel inferior para confirmar a quantidade, com prévia ao vivo de kcal e macros. */
public class QuantityInputSheet extends BottomSheetDialogFragment {

    public static final String TAG = "QuantityInputSheet";
    public static final String RESULT_KEY = "quantity_result";
    public static final String RESULT_NAME = "name";
    public static final String RESULT_GRAMS = "grams";

    private SheetQuantityBinding binding;
    private DiaryViewModel viewModel;
    private Food food;
    private boolean filled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetQuantityBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        filled = savedInstanceState != null;
        label(binding.kcal, R.string.nutrient_kcal);
        label(binding.protein, R.string.nutrient_protein);
        label(binding.carbohydrate, R.string.nutrient_carbohydrate);
        label(binding.fat, R.string.nutrient_fat);

        viewModel = new ViewModelProvider(requireParentFragment(),
                DiaryViewModel.factory(((CaLjuApp) requireActivity().getApplication()).container()))
                .get(DiaryViewModel.class);
        viewModel.selected().observe(getViewLifecycleOwner(), this::show);

        binding.grams.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                binding.gramsLayout.setError(null);
                updatePreview();
            }
        });
        binding.grams.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                confirm();
                return true;
            }
            return false;
        });
        binding.addButton.setOnClickListener(v -> confirm());
    }

    private void show(SelectedFood selected) {
        if (selected == null || selected.food() == null) {
            return;
        }
        food = selected.food();
        binding.foodName.setText(food.name());
        if (food.portion().isPresent()) {
            Portion portion = food.portion().get();
            binding.portionButton.setVisibility(View.VISIBLE);
            binding.portionButton.setText(getString(R.string.quantity_use_portion,
                    portion.label(), Formats.grams(portion.grams())));
            binding.portionButton.setOnClickListener(v -> setGrams(portion.grams()));
        }
        if (!filled) {
            filled = true;
            if (selected.suggestedGrams() != null) {
                setGrams(selected.suggestedGrams());
            } else {
                binding.grams.requestFocus();
            }
        }
        updatePreview();
    }

    private void setGrams(BigDecimal grams) {
        String text = grams.stripTrailingZeros().toPlainString().replace('.', ',');
        binding.grams.setText(text);
        binding.grams.setSelection(text.length());
    }

    private void updatePreview() {
        BigDecimal grams = Formats.parsePositive(String.valueOf(binding.grams.getText()));
        value(binding.kcal, grams, Nutrients.ENERGY, NutrientUnit.KCAL);
        value(binding.protein, grams, Nutrients.PROTEIN, NutrientUnit.GRAM);
        value(binding.carbohydrate, grams, Nutrients.CARBOHYDRATE, NutrientUnit.GRAM);
        value(binding.fat, grams, Nutrients.FAT, NutrientUnit.GRAM);
    }

    private void value(ViewNutrientValueBinding view, BigDecimal grams, String code, NutrientUnit unit) {
        if (food == null || grams == null) {
            view.value.setText(R.string.value_unknown);
            return;
        }
        NutrientTotal total = NutritionCalculator.total(Collections.singletonList(new Intake(food, grams)), code);
        view.value.setText(total.complete() ? Formats.amount(total.amount(), unit) : getString(R.string.value_unknown));
    }

    private void label(ViewNutrientValueBinding view, int text) {
        view.label.setText(text);
        view.value.setText(R.string.value_unknown);
    }

    private void confirm() {
        BigDecimal grams = Formats.parsePositive(String.valueOf(binding.grams.getText()));
        if (food == null) {
            return;
        }
        if (grams == null) {
            binding.gramsLayout.setError(getString(R.string.quantity_invalid));
            return;
        }
        Bundle result = new Bundle();
        result.putString(RESULT_NAME, food.name());
        result.putString(RESULT_GRAMS, grams.toPlainString());
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        dismiss();
    }

    @Override
    public void onDismiss(@NonNull DialogInterface dialog) {
        super.onDismiss(dialog);
        if (viewModel != null && !requireActivity().isChangingConfigurations()) {
            viewModel.clearSelection();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
