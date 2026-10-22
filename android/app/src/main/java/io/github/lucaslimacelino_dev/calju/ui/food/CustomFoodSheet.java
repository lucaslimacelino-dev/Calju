package io.github.lucaslimacelino_dev.calju.ui.food;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.Map;

import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.data.repository.CustomFoodData;
import io.github.lucaslimacelino_dev.calju.databinding.SheetCustomFoodBinding;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionLabel;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

/**
 * Cadastro de um alimento do usuário a partir da tabela nutricional da embalagem, digitada
 * "por 100 g" ou "por porção" (convertida para 100 g ao salvar). Devolve o resultado para quem
 * abriu a tela pela chave {@link #RESULT_KEY}.
 */
public class CustomFoodSheet extends BottomSheetDialogFragment {

    public static final String TAG = "CustomFoodSheet";
    public static final String RESULT_KEY = "custom_food";
    public static final String RESULT_FOOD_ID = "food_id";
    public static final String RESULT_NAME = "name";
    public static final String RESULT_CREATED = "created";
    public static final String RESULT_DELETED = "deleted";

    private static final String ARG_FOOD_ID = "food_id";
    private static final String ARG_NAME = "name";
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private SheetCustomFoodBinding binding;
    private CustomFoodViewModel viewModel;
    private String foodId;
    private boolean filled;

    public static CustomFoodSheet newFood(String initialName) {
        CustomFoodSheet sheet = new CustomFoodSheet();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, initialName);
        sheet.setArguments(args);
        return sheet;
    }

    public static CustomFoodSheet edit(String foodId) {
        CustomFoodSheet sheet = new CustomFoodSheet();
        Bundle args = new Bundle();
        args.putString(ARG_FOOD_ID, foodId);
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetCustomFoodBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        filled = savedInstanceState != null;
        Bundle args = requireArguments();
        foodId = args.getString(ARG_FOOD_ID);
        viewModel = new ViewModelProvider(this,
                CustomFoodViewModel.factory(((CaLjuApp) requireActivity().getApplication()).container()))
                .get(CustomFoodViewModel.class);

        if (foodId != null) {
            binding.title.setText(R.string.food_edit_title);
            binding.deleteButton.setVisibility(View.VISIBLE);
            viewModel.loaded().observe(getViewLifecycleOwner(), this::fill);
            viewModel.load(foodId);
        } else if (!filled) {
            filled = true;
            binding.name.setText(args.getString(ARG_NAME));
        }

        binding.basisGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> showBasis());
        showBasis();
        binding.sodium.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                save();
                return true;
            }
            return false;
        });
        binding.saveButton.setOnClickListener(v -> save());
        binding.deleteButton.setOnClickListener(v -> {
            binding.deleteButton.setEnabled(false);
            viewModel.delete(foodId, text(binding.name).trim());
        });
        viewModel.done().observe(getViewLifecycleOwner(), done -> {
            if (done == null) {
                return;
            }
            viewModel.doneHandled();
            Bundle result = new Bundle();
            result.putString(RESULT_FOOD_ID, done.foodId());
            result.putString(RESULT_NAME, done.name());
            result.putBoolean(RESULT_CREATED, done.created());
            result.putBoolean(RESULT_DELETED, done.deleted());
            getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
            dismiss();
        });
    }

    /** O formulário é longo: abre já expandido, sem a etapa "meio aberto". */
    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetBehavior<?> behavior = ((BottomSheetDialog) dialog).getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private boolean perPortion() {
        return binding.basisGroup.getCheckedButtonId() == R.id.basis_portion;
    }

    /** "Por porção" torna o peso da porção obrigatório; "por 100 g" o deixa opcional. */
    private void showBasis() {
        binding.portionHint.setVisibility(perPortion() ? View.GONE : View.VISIBLE);
        binding.portionGramsLayout.setError(null);
    }

    /** Edição: preenche uma vez com os valores gravados (sempre por 100 g). */
    private void fill(CustomFoodData data) {
        if (filled || data == null) {
            return;
        }
        filled = true;
        binding.name.setText(data.name());
        binding.brand.setText(data.brand());
        binding.basisGroup.check(R.id.basis_100g);
        binding.portionLabel.setText(data.portionLabel());
        set(binding.portionGrams, data.portionGrams());
        Map<String, BigDecimal> values = data.per100g();
        set(binding.kcal, values.get(Nutrients.ENERGY));
        set(binding.protein, values.get(Nutrients.PROTEIN));
        set(binding.carbohydrate, values.get(Nutrients.CARBOHYDRATE));
        set(binding.fat, values.get(Nutrients.FAT));
        set(binding.fiber, values.get(Nutrients.FIBER));
        set(binding.sodium, values.get(Nutrients.SODIUM));
    }

    private static void set(TextInputEditText field, BigDecimal value) {
        field.setText(value == null ? "" : value.stripTrailingZeros().toPlainString().replace('.', ','));
    }

    private void save() {
        binding.formError.setVisibility(View.GONE);
        String name = text(binding.name).trim();
        binding.nameLayout.setError(name.isEmpty() ? getString(R.string.food_required) : null);
        boolean valid = !name.isEmpty();

        BigDecimal portionGrams = Formats.parsePositive(text(binding.portionGrams));
        String gramsText = text(binding.portionGrams).trim();
        String portionLabel = text(binding.portionLabel).trim();
        String portionError = null;
        if (!gramsText.isEmpty() && portionGrams == null) {
            portionError = getString(R.string.food_invalid_number);
        } else if (portionGrams == null && (perPortion() || !portionLabel.isEmpty())) {
            portionError = getString(R.string.food_portion_required);
        }
        binding.portionGramsLayout.setError(portionError);
        valid &= portionError == null;
        if (portionGrams != null && portionLabel.isEmpty()) {
            portionLabel = getString(R.string.food_portion_label_default);
        }

        BigDecimal kcal = required(binding.kcalLayout, binding.kcal);
        BigDecimal protein = required(binding.proteinLayout, binding.protein);
        BigDecimal carbohydrate = required(binding.carbohydrateLayout, binding.carbohydrate);
        BigDecimal fat = required(binding.fatLayout, binding.fat);
        BigDecimal fiber = optional(binding.fiberLayout, binding.fiber);
        BigDecimal sodium = optional(binding.sodiumLayout, binding.sodium);
        for (TextInputLayout layout : new TextInputLayout[]{binding.kcalLayout, binding.proteinLayout,
                binding.carbohydrateLayout, binding.fatLayout, binding.fiberLayout, binding.sodiumLayout}) {
            valid &= layout.getError() == null;
        }
        if (!valid) {
            return;
        }

        NutritionLabel label = new NutritionLabel(perPortion() ? portionGrams : HUNDRED,
                kcal, protein, carbohydrate, fat, fiber, sodium);
        if (!label.validate().isEmpty()) {
            binding.formError.setVisibility(View.VISIBLE);
            return;
        }
        WindowCompat.getInsetsController(requireActivity().getWindow(), binding.getRoot())
                .hide(WindowInsetsCompat.Type.ime());
        binding.saveButton.setEnabled(false);
        viewModel.save(foodId, new CustomFoodData(name, text(binding.brand).trim(), label.per100g(),
                portionGrams == null ? null : portionLabel, portionGrams));
    }

    private BigDecimal required(TextInputLayout layout, TextInputEditText field) {
        String value = text(field).trim();
        BigDecimal parsed = Formats.parseNonNegative(value);
        layout.setError(value.isEmpty() ? getString(R.string.food_required)
                : parsed == null ? getString(R.string.food_invalid_number) : null);
        return parsed;
    }

    /** Vazio = sem informação (nulo, não zero). */
    private BigDecimal optional(TextInputLayout layout, TextInputEditText field) {
        String value = text(field).trim();
        BigDecimal parsed = Formats.parseNonNegative(value);
        layout.setError(!value.isEmpty() && parsed == null ? getString(R.string.food_invalid_number) : null);
        return parsed;
    }

    private static String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
