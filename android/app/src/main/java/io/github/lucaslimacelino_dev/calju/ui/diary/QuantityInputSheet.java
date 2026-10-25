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
import java.util.Locale;

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
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import io.github.lucaslimacelino_dev.calju.domain.Portion;
import io.github.lucaslimacelino_dev.calju.domain.PortionText;
import io.github.lucaslimacelino_dev.calju.domain.QuantityUnit;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

/**
 * Painel inferior para confirmar quantidade e ocasião, com prévia ao vivo de kcal e macros.
 * Serve tanto para um registro novo quanto para editar ou remover um existente. Alimentos com
 * porção caseira podem ser medidos em porções ("4 unidades") ou em gramas.
 */
public class QuantityInputSheet extends BottomSheetDialogFragment {

    public static final String TAG = "QuantityInputSheet";
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private SheetQuantityBinding binding;
    private DiaryViewModel viewModel;
    private SelectedFood selected;
    private Portion portion;
    private boolean filled;
    private boolean switching;

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
        binding.unitGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked && !switching) {
                switchUnit(checkedId == R.id.unit_portion);
            }
        });
        binding.addButton.setOnClickListener(v -> confirm());
        binding.removeButton.setOnClickListener(v -> {
            if (selected != null && selected.isEdit()) {
                viewModel.removeEntry(selected.entryId(), selected.occasion());
                dismiss();
            }
        });
    }

    private void show(SelectedFood value) {
        if (value == null || value.food() == null) {
            return;
        }
        selected = value;
        Food food = value.food();
        portion = food.portion().orElse(null);
        binding.foodName.setText(food.name());
        binding.addButton.setText(value.isEdit() ? R.string.quantity_save : R.string.quantity_add);
        binding.removeButton.setVisibility(value.isEdit() ? View.VISIBLE : View.GONE);
        binding.unitGroup.setVisibility(portion != null ? View.VISIBLE : View.GONE);
        if (portion != null) {
            String plural = PortionText.noun(portion.label(), BigDecimal.TEN);
            binding.unitPortion.setText(plural.substring(0, 1).toUpperCase(PT_BR) + plural.substring(1));
            binding.unitWeight.setText(food.unit() == QuantityUnit.MILLILITER
                    ? R.string.quantity_milliliters : R.string.quantity_grams);
        }
        if (!filled) {
            filled = true;
            binding.occasionGroup.check(Occasions.chip(value.occasion()));
            boolean units = portion != null && value.preferUnits();
            check(units);
            if (value.suggestedGrams() != null) {
                setValue(units ? PortionText.exactCount(value.suggestedGrams(), portion.grams())
                        : value.suggestedGrams());
            } else {
                binding.grams.requestFocus();
            }
        }
        updatePreview();
    }

    private boolean unitsMode() {
        return portion != null && binding.unitGroup.getCheckedButtonId() == R.id.unit_portion;
    }

    private void check(boolean units) {
        switching = true;
        binding.unitGroup.check(units ? R.id.unit_portion : R.id.unit_weight);
        switching = false;
    }

    /** Troca entre porções e gramas mantendo a mesma quantidade. */
    private void switchUnit(boolean toUnits) {
        BigDecimal typed = Formats.parsePositive(String.valueOf(binding.grams.getText()));
        if (typed != null && portion != null) {
            setValue(toUnits ? PortionText.exactCount(typed, portion.grams()) : typed.multiply(portion.grams()));
        }
        updatePreview();
    }

    private void setValue(BigDecimal value) {
        String text = value.stripTrailingZeros().toPlainString().replace('.', ',');
        binding.grams.setText(text);
        binding.grams.setSelection(text.length());
    }

    /** O que foi digitado, convertido em gramas (porções vezes o peso da porção). */
    private BigDecimal grams() {
        BigDecimal typed = Formats.parsePositive(String.valueOf(binding.grams.getText()));
        if (typed == null || !unitsMode()) {
            return typed;
        }
        return typed.multiply(portion.grams());
    }

    private void updatePreview() {
        if (selected == null) {
            return;
        }
        QuantityUnit unit = selected.food().unit();
        BigDecimal grams = grams();
        if (unitsMode()) {
            BigDecimal typed = Formats.parsePositive(String.valueOf(binding.grams.getText()));
            binding.gramsLayout.setSuffixText(PortionText.noun(portion.label(), typed));
            binding.gramsLayout.setHelperText(grams == null ? null
                    : getString(R.string.quantity_equals, Formats.quantity(grams, unit)));
        } else {
            binding.gramsLayout.setSuffixText(unit.symbol());
            binding.gramsLayout.setHelperText(null);
        }
        value(binding.kcal, grams, Nutrients.ENERGY, NutrientUnit.KCAL);
        value(binding.protein, grams, Nutrients.PROTEIN, NutrientUnit.GRAM);
        value(binding.carbohydrate, grams, Nutrients.CARBOHYDRATE, NutrientUnit.GRAM);
        value(binding.fat, grams, Nutrients.FAT, NutrientUnit.GRAM);
    }

    private void value(ViewNutrientValueBinding view, BigDecimal grams, String code, NutrientUnit unit) {
        if (selected == null || grams == null) {
            view.value.setText(R.string.value_unknown);
            return;
        }
        NutrientTotal total = NutritionCalculator.total(
                Collections.singletonList(new Intake(selected.food(), grams)), code);
        view.value.setText(total.complete() ? Formats.amount(total.amount(), unit) : getString(R.string.value_unknown));
    }

    private void label(ViewNutrientValueBinding view, int text) {
        view.label.setText(text);
        view.value.setText(R.string.value_unknown);
    }

    private void confirm() {
        if (selected == null) {
            return;
        }
        BigDecimal grams = grams();
        if (grams == null) {
            binding.gramsLayout.setError(getString(R.string.quantity_invalid));
            return;
        }
        Occasion occasion = Occasions.fromChip(binding.occasionGroup.getCheckedChipId());
        if (selected.isEdit()) {
            viewModel.updateItem(selected.itemId(), grams, occasion);
        } else {
            viewModel.addFood(selected.foodId(), grams, occasion);
        }
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
