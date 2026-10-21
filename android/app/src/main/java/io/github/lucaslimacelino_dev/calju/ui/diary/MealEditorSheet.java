package io.github.lucaslimacelino_dev.calju.ui.diary;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.List;
import java.util.function.Consumer;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.data.repository.ItemDraft;
import io.github.lucaslimacelino_dev.calju.databinding.ItemEditorRowBinding;
import io.github.lucaslimacelino_dev.calju.databinding.SheetMealEditorBinding;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

/**
 * Editor de refeição, em três modos:
 * nova refeição; refeição salva (salvar o molde ou adicionar hoje com ajustes); registro do
 * diário com vários itens (ajustar ou remover só aquele registro).
 */
public class MealEditorSheet extends BottomSheetDialogFragment {

    public static final String TAG = "MealEditorSheet";
    private static final String ARG_MODE = "mode";
    private static final String ARG_ID = "id";

    private SheetMealEditorBinding binding;
    private MealEditorViewModel editor;
    private DiaryViewModel diary;
    private MealEditorViewModel.Mode mode;
    private String id;
    private boolean filled;

    public static MealEditorSheet newMeal() {
        return create(MealEditorViewModel.Mode.NEW_MEAL, null);
    }

    public static MealEditorSheet editMeal(String mealId) {
        return create(MealEditorViewModel.Mode.EDIT_MEAL, mealId);
    }

    public static MealEditorSheet editEntry(String entryId) {
        return create(MealEditorViewModel.Mode.ENTRY, entryId);
    }

    private static MealEditorSheet create(MealEditorViewModel.Mode mode, String id) {
        Bundle args = new Bundle();
        args.putString(ARG_MODE, mode.name());
        args.putString(ARG_ID, id);
        MealEditorSheet sheet = new MealEditorSheet();
        sheet.setArguments(args);
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetMealEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        filled = savedInstanceState != null;
        mode = MealEditorViewModel.Mode.valueOf(requireArguments().getString(ARG_MODE));
        id = requireArguments().getString(ARG_ID);
        AppContainer container = ((CaLjuApp) requireActivity().getApplication()).container();
        editor = new ViewModelProvider(this, MealEditorViewModel.factory(container)).get(MealEditorViewModel.class);
        diary = new ViewModelProvider(requireParentFragment(), DiaryViewModel.factory(container))
                .get(DiaryViewModel.class);

        configureForMode();
        editor.load(mode, id);
        editor.items().observe(getViewLifecycleOwner(), this::renderItems);
        editor.total().observe(getViewLifecycleOwner(), this::renderTotal);
        editor.name().observe(getViewLifecycleOwner(), name -> {
            if (mode == MealEditorViewModel.Mode.ENTRY && name != null) {
                binding.title.setText(name);
            }
            if (!filled && name != null) {
                binding.name.setText(name);
            }
        });
        editor.occasion().observe(getViewLifecycleOwner(), occasion -> {
            if (!filled && occasion != null) {
                filled = true;
                binding.occasionGroup.check(Occasions.chip(occasion));
            }
        });

        SuggestionAdapter suggestions = new SuggestionAdapter(suggestion -> {
            binding.search.setText("");
            editor.include(suggestion);
        });
        binding.suggestions.setAdapter(suggestions);
        editor.suggestions().observe(getViewLifecycleOwner(), suggestions::submitList);
        binding.search.addTextChangedListener(afterTextChanged(text -> editor.search(text)));
    }

    private void configureForMode() {
        boolean isMeal = mode != MealEditorViewModel.Mode.ENTRY;
        boolean canAddToday = mode != MealEditorViewModel.Mode.NEW_MEAL;
        binding.nameLayout.setVisibility(isMeal ? View.VISIBLE : View.GONE);
        binding.occasionGroup.setVisibility(canAddToday ? View.VISIBLE : View.GONE);
        binding.hint.setVisibility(mode == MealEditorViewModel.Mode.EDIT_MEAL ? View.VISIBLE : View.GONE);
        binding.duplicateButton.setVisibility(mode == MealEditorViewModel.Mode.EDIT_MEAL ? View.VISIBLE : View.GONE);
        binding.deleteButton.setVisibility(canAddToday ? View.VISIBLE : View.GONE);

        switch (mode) {
            case NEW_MEAL:
                binding.title.setText(R.string.meal_new_title);
                binding.primaryButton.setText(R.string.meal_save);
                binding.primaryButton.setOnClickListener(v -> saveMeal());
                break;
            case EDIT_MEAL:
                binding.title.setText(R.string.meal_edit_title);
                binding.primaryButton.setText(R.string.meal_add_today_button);
                binding.primaryButton.setOnClickListener(v -> addToday());
                binding.secondaryButton.setVisibility(View.VISIBLE);
                binding.secondaryButton.setText(R.string.meal_save);
                binding.secondaryButton.setOnClickListener(v -> saveMeal());
                binding.duplicateButton.setOnClickListener(v -> {
                    diary.duplicateMeal(id, getString(R.string.meal_copy_name, name()));
                    dismiss();
                });
                binding.deleteButton.setText(R.string.meal_delete);
                binding.deleteButton.setOnClickListener(v -> {
                    diary.deleteMeal(id, name());
                    dismiss();
                });
                break;
            case ENTRY:
                binding.primaryButton.setText(R.string.entry_edit_save);
                binding.primaryButton.setOnClickListener(v -> saveEntry());
                binding.deleteButton.setText(R.string.quantity_remove);
                binding.deleteButton.setOnClickListener(v -> {
                    diary.removeEntry(id, occasion());
                    dismiss();
                });
                break;
        }
    }

    private void renderItems(List<EditorItem> items) {
        binding.items.removeAllViews();
        binding.itemsError.setVisibility(View.GONE);
        for (EditorItem item : items) {
            ItemEditorRowBinding row = ItemEditorRowBinding.inflate(getLayoutInflater(), binding.items, true);
            row.name.setText(item.food.name());
            row.grams.setText(item.gramsText);
            row.grams.addTextChangedListener(afterTextChanged(text -> {
                row.grams.setError(null);
                editor.gramsChanged(item.key, text);
            }));
            row.remove.setContentDescription(getString(R.string.meal_remove_item, item.food.name()));
            row.remove.setOnClickListener(v -> editor.remove(item.key));
            if (item.gramsText.isEmpty()) {
                row.grams.requestFocus();
            }
        }
    }

    private void renderTotal(NutrientTotal total) {
        String value = Formats.amount(total.amount(), NutrientUnit.KCAL);
        binding.total.setText(getString(R.string.meal_total,
                total.complete() ? value : getString(R.string.value_at_least, value)));
    }

    /** Valida a lista; marca em vermelho as quantidades inválidas. Nulo se algo estiver errado. */
    private List<ItemDraft> validItems() {
        List<EditorItem> items = editor.items().getValue();
        if (items == null || items.isEmpty()) {
            binding.itemsError.setVisibility(View.VISIBLE);
            return null;
        }
        List<ItemDraft> drafts = editor.drafts();
        if (drafts == null) {
            for (int i = 0; i < binding.items.getChildCount(); i++) {
                ItemEditorRowBinding row = ItemEditorRowBinding.bind(binding.items.getChildAt(i));
                if (Formats.parsePositive(String.valueOf(row.grams.getText())) == null) {
                    row.grams.setError(getString(R.string.quantity_invalid));
                }
            }
        }
        return drafts;
    }

    private String validName() {
        String name = name();
        binding.nameLayout.setError(name.isEmpty() ? getString(R.string.meal_name_required) : null);
        return name.isEmpty() ? null : name;
    }

    private void saveMeal() {
        String name = validName();
        List<ItemDraft> items = validItems();
        if (name != null && items != null) {
            diary.saveMeal(mode == MealEditorViewModel.Mode.NEW_MEAL ? null : id, name, items);
            dismiss();
        }
    }

    private void addToday() {
        List<ItemDraft> items = validItems();
        if (items != null) {
            diary.addMealWithChanges(id, name().isEmpty() ? null : name(), occasion(), items);
            dismiss();
        }
    }

    private void saveEntry() {
        List<ItemDraft> items = validItems();
        if (items != null) {
            diary.updateEntry(id, occasion(), items);
            dismiss();
        }
    }

    private String name() {
        return binding.name.getText() == null ? "" : binding.name.getText().toString().trim();
    }

    private Occasion occasion() {
        return Occasions.fromChip(binding.occasionGroup.getCheckedChipId());
    }

    private static TextWatcher afterTextChanged(Consumer<String> action) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                action.accept(s.toString());
            }
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
