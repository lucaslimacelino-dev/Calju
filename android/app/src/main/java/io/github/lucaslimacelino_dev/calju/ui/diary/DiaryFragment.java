package io.github.lucaslimacelino_dev.calju.ui.diary;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.function.Function;

import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.FragmentDiaryBinding;
import io.github.lucaslimacelino_dev.calju.databinding.ItemLogEntryBinding;
import io.github.lucaslimacelino_dev.calju.databinding.ItemOccasionSectionBinding;
import io.github.lucaslimacelino_dev.calju.databinding.ItemSavedMealBinding;
import io.github.lucaslimacelino_dev.calju.databinding.ViewMacroCardBinding;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import io.github.lucaslimacelino_dev.calju.ui.DateRequest;
import io.github.lucaslimacelino_dev.calju.ui.Dates;
import io.github.lucaslimacelino_dev.calju.ui.Formats;
import io.github.lucaslimacelino_dev.calju.ui.components.NumberAnimator;

public class DiaryFragment extends Fragment {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private FragmentDiaryBinding binding;
    private DiaryViewModel viewModel;
    private SuggestionAdapter suggestionAdapter;
    private final Map<Occasion, ItemOccasionSectionBinding> sectionCards = new EnumMap<>(Occasion.class);

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDiaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this,
                DiaryViewModel.factory(((CaLjuApp) requireActivity().getApplication()).container()))
                .get(DiaryViewModel.class);

        binding.protein.label.setText(R.string.macro_protein);
        binding.carbohydrate.label.setText(R.string.macro_carbohydrate);
        binding.fat.label.setText(R.string.macro_fat);
        tintDot(binding.protein, R.color.summary_protein);
        tintDot(binding.carbohydrate, R.color.summary_carbohydrate);
        tintDot(binding.fat, R.color.summary_fat);

        suggestionAdapter = new SuggestionAdapter(viewModel::select);
        createSections();
        binding.suggestions.setAdapter(suggestionAdapter);

        viewModel.dayState().observe(getViewLifecycleOwner(), this::showDay);
        viewModel.date().observe(getViewLifecycleOwner(), this::showDate);
        DateRequest dateRequest = new ViewModelProvider(requireActivity()).get(DateRequest.class);
        dateRequest.requested().observe(getViewLifecycleOwner(), date -> {
            if (date != null) {
                viewModel.showDate(date);
                dateRequest.consumed();
            }
        });
        viewModel.suggestions().observe(getViewLifecycleOwner(), this::showSuggestions);
        viewModel.target().observe(getViewLifecycleOwner(), occasion -> binding.foodInput.setHint(occasion == null
                ? getString(R.string.food_input_hint)
                : getString(R.string.food_input_hint_for, lowerLabel(occasion))));
        viewModel.events().observe(getViewLifecycleOwner(), this::showEvent);
        viewModel.mealSummaries().observe(getViewLifecycleOwner(), meals ->
                showMeals(meals, Boolean.TRUE.equals(viewModel.mealsExpanded().getValue())));
        viewModel.mealsExpanded().observe(getViewLifecycleOwner(), expanded -> {
            List<MealSummary> meals = viewModel.mealSummaries().getValue();
            showMeals(meals != null ? meals : Collections.emptyList(), Boolean.TRUE.equals(expanded));
        });
        binding.mealsHeader.setOnClickListener(v -> viewModel.toggleMeals());
        binding.newMeal.setOnClickListener(v -> openMealEditor(MealEditorSheet.newMeal()));
        viewModel.selected().observe(getViewLifecycleOwner(), selected -> {
            if (selected != null && getChildFragmentManager().findFragmentByTag(QuantityInputSheet.TAG) == null) {
                hideKeyboard();
                new QuantityInputSheet().show(getChildFragmentManager(), QuantityInputSheet.TAG);
            }
        });

        binding.previousDay.setOnClickListener(v -> viewModel.previousDay());
        binding.nextDay.setOnClickListener(v -> viewModel.nextDay());
        // Seleciona a aba (em vez de navigate()) para a barra inferior continuar no controle da navegação.
        binding.defineGoal.setOnClickListener(v -> requireActivity()
                .<BottomNavigationView>findViewById(R.id.bottom_navigation)
                .setSelectedItemId(R.id.profileFragment));
        binding.micronutrientsButton.setOnClickListener(v ->
                new MicronutrientsSheet().show(getChildFragmentManager(), MicronutrientsSheet.TAG));

        binding.foodInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.onTextChanged(s.toString());
            }
        });
        binding.foodInput.setOnEditorActionListener((v, actionId, event) -> {
            boolean send = actionId == EditorInfo.IME_ACTION_SEND
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
            if (!send) {
                return false;
            }
            // Teclado físico envia o Enter duas vezes (apertar e soltar); só o "apertar" conta.
            if (event == null || event.getAction() == KeyEvent.ACTION_DOWN) {
                selectFirstSuggestion();
            }
            return true;
        });
        binding.sendButton.setOnClickListener(v -> selectFirstSuggestion());
    }

    private void showDate(LocalDate date) {
        LocalDate today = viewModel.today();
        binding.dayTitle.setText(Dates.title(requireContext(), date, today));
        binding.daySubtitle.setText(Dates.full(date));
        binding.nextDay.setEnabled(date.isBefore(today));
    }

    private void showDay(DayState state) {
        showEnergy(state.kcal(), state.goal());
        Goal goal = state.goal();
        macro(binding.protein, state.protein(), goal == null ? null : goal.proteinG(), R.color.summary_protein);
        macro(binding.carbohydrate, state.carbohydrate(), goal == null ? null : goal.carbohydrateG(),
                R.color.summary_carbohydrate);
        macro(binding.fat, state.fat(), goal == null ? null : goal.fatG(), R.color.summary_fat);

        binding.emptyState.setVisibility(state.entries().isEmpty() ? View.VISIBLE : View.GONE);
        showSections(OccasionSection.group(state.entries()));
    }

    /** As quatro seções são criadas uma vez; as atualizações só trocam o conteúdo (a tela não pula). */
    private void createSections() {
        sectionCards.clear();
        for (Occasion occasion : Occasion.values()) {
            ItemOccasionSectionBinding card = ItemOccasionSectionBinding.inflate(getLayoutInflater(),
                    binding.entries, true);
            card.icon.setImageResource(Occasions.icon(occasion));
            card.icon.setImageTintList(colorList(Occasions.color(occasion)));
            card.icon.setBackgroundTintList(colorList(Occasions.container(occasion)));
            card.title.setText(Occasions.label(occasion));
            card.add.setContentDescription(getString(R.string.section_add, lowerLabel(occasion)));
            card.add.setOnClickListener(v -> startAdding(occasion));
            sectionCards.put(occasion, card);
        }
    }

    private void showSections(List<OccasionSection> sections) {
        for (OccasionSection section : sections) {
            ItemOccasionSectionBinding card = sectionCards.get(section.occasion());
            card.kcal.setVisibility(section.isEmpty() ? View.GONE : View.VISIBLE);
            card.kcal.setText(getString(R.string.entry_kcal, mark(section.complete(), R.string.value_at_least,
                    value -> Formats.amount(value, NutrientUnit.KCAL)).apply(section.kcal())));
            card.items.removeAllViews();
            card.items.setVisibility(section.isEmpty() ? View.GONE : View.VISIBLE);
            for (EntryRow row : section.entries()) {
                bindEntry(ItemLogEntryBinding.inflate(getLayoutInflater(), card.items, true), row);
            }
        }
    }

    private void bindEntry(ItemLogEntryBinding item, EntryRow row) {
        item.foodName.setText(row.title());
        String grams = Formats.grams(row.totalGrams());
        item.grams.setText(row.isSingleItem() ? grams
                : getResources().getQuantityString(R.plurals.entry_items, row.itemCount(), row.itemCount(), grams));
        item.kcal.setText(row.kcal() == null
                ? getString(R.string.value_unknown)
                : getString(R.string.entry_kcal, Formats.amount(row.kcal(), NutrientUnit.KCAL)));
        item.getRoot().setOnClickListener(v -> onEntryClicked(row));
    }

    /** "+" de uma seção: o próximo registro vai para essa refeição; o campo abre com o teclado. */
    private void startAdding(Occasion occasion) {
        viewModel.setTarget(occasion);
        binding.foodInput.requestFocus();
        WindowCompat.getInsetsController(requireActivity().getWindow(), binding.foodInput)
                .show(WindowInsetsCompat.Type.ime());
    }

    private String lowerLabel(Occasion occasion) {
        return getString(Occasions.label(occasion)).toLowerCase(PT_BR);
    }

    private ColorStateList colorList(int colorRes) {
        return ColorStateList.valueOf(ContextCompat.getColor(requireContext(), colorRes));
    }

    private void tintDot(ViewMacroCardBinding card, int colorRes) {
        TextViewCompat.setCompoundDrawableTintList(card.label, colorList(colorRes));
    }

    /**
     * Sem meta: mostra as kcal consumidas. Com meta: as restantes (ou o excesso) e o anel enche.
     * Se algum alimento não tem energia medida, o consumo é "pelo menos" e o restante "no máximo".
     */
    private void showEnergy(NutrientTotal kcal, Goal goal) {
        String consumed = Formats.amount(kcal.amount(), NutrientUnit.KCAL);
        binding.defineGoal.setVisibility(goal == null ? View.VISIBLE : View.GONE);
        if (goal == null) {
            NumberAnimator.animate(binding.kcalValue, kcal.amount(), mark(kcal.complete(), R.string.value_at_least,
                    value -> Formats.amount(value, NutrientUnit.KCAL)));
            binding.kcalCaption.setText(R.string.kcal_consumed);
            binding.ring.setProgress(0f);
            binding.ringContainer.setContentDescription(getString(R.string.kcal_ring_description, consumed));
            return;
        }
        String target = Formats.amount(goal.kcal(), NutrientUnit.KCAL);
        BigDecimal remaining = goal.kcal().subtract(kcal.amount());
        boolean over = remaining.signum() < 0;
        NumberAnimator.animate(binding.kcalValue, remaining.abs(), mark(kcal.complete(),
                over ? R.string.value_at_least : R.string.value_at_most,
                value -> Formats.amount(value, NutrientUnit.KCAL)));
        binding.kcalCaption.setText(getString(over ? R.string.kcal_over : R.string.kcal_remaining, target));
        binding.ring.setProgress(kcal.amount().floatValue() / goal.kcal().floatValue());
        binding.ringContainer.setContentDescription(
                getString(R.string.kcal_ring_description_goal, consumed, target) + ". "
                        + binding.kcalValue.getText() + " " + binding.kcalCaption.getText());
    }

    private void macro(ViewMacroCardBinding card, NutrientTotal total, BigDecimal goalGrams, int color) {
        NumberAnimator.animate(card.value, total.amount(), mark(total.complete(), R.string.value_at_least,
                value -> getString(R.string.value_grams, Formats.amount(value, NutrientUnit.GRAM))));
        boolean hasGoal = goalGrams != null;
        card.goal.setVisibility(hasGoal ? View.VISIBLE : View.GONE);
        card.bar.setVisibility(hasGoal ? View.VISIBLE : View.GONE);
        if (hasGoal) {
            card.goal.setText(getString(R.string.macro_goal, Formats.count(goalGrams)));
            card.bar.setIndicatorColor(ContextCompat.getColor(requireContext(), color));
            int percent = Math.min(100, Math.round(total.amount().floatValue() / goalGrams.floatValue() * 100));
            card.bar.setProgressCompat(percent, true);
        }
    }

    /** Total incompleto ganha um prefixo ("≥" ou "≤"): algum alimento não tem o valor medido. */
    private Function<BigDecimal, String> mark(boolean complete, int prefix, Function<BigDecimal, String> format) {
        return value -> complete ? format.apply(value) : getString(prefix, format.apply(value));
    }

    private void showSuggestions(List<Suggestion> suggestions) {
        suggestionAdapter.submitList(suggestions);
        binding.suggestionsCard.setVisibility(suggestions.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void showEvent(DiaryEvent event) {
        if (event == null || !event.consume()) {
            return;
        }
        String message;
        switch (event.type()) {
            case ADDED:
                binding.foodInput.setText("");
                viewModel.clearSuggestions();
                viewModel.clearTarget();
                hideKeyboard();
                message = getString(R.string.event_added, lowerLabel(event.occasion()));
                break;
            case MEAL_SAVED:
                message = getString(R.string.event_meal_saved, event.name());
                break;
            case MEAL_DUPLICATED:
                message = getString(R.string.event_meal_duplicated, event.name());
                break;
            case MEAL_DELETED:
                message = getString(R.string.event_meal_deleted, event.name());
                break;
            default:
                message = getString(R.string.event_removed);
                break;
        }
        // O CoordinatorLayout da área de conteúdo posiciona o aviso logo acima do campo.
        Snackbar snackbar = Snackbar.make(binding.contentArea, message, Snackbar.LENGTH_LONG);
        if (event.canUndo()) {
            snackbar.setAction(R.string.undo, v -> viewModel.undo(event));
        }
        snackbar.show();
    }

    private void onEntryClicked(EntryRow row) {
        if (row.isSingleItem()) {
            viewModel.edit(row);
        } else if (getChildFragmentManager().findFragmentByTag(MealEditorSheet.TAG) == null) {
            MealEditorSheet.editEntry(row.entryId()).show(getChildFragmentManager(), MealEditorSheet.TAG);
        }
    }

    private void openMealEditor(MealEditorSheet sheet) {
        if (getChildFragmentManager().findFragmentByTag(MealEditorSheet.TAG) == null) {
            sheet.show(getChildFragmentManager(), MealEditorSheet.TAG);
        }
    }

    /** Seção "Refeições prontas": cabeçalho sempre visível; lista só quando aberta. */
    private void showMeals(List<MealSummary> meals, boolean expanded) {
        String count = getResources().getQuantityString(R.plurals.meals_count, meals.size(), meals.size());
        binding.mealsCount.setText(getString(expanded ? R.string.meals_count_open : R.string.meals_count_closed, count));
        binding.mealsHeader.setContentDescription(getString(expanded ? R.string.meals_collapse : R.string.meals_expand));
        binding.mealsList.setVisibility(expanded ? View.VISIBLE : View.GONE);
        binding.newMeal.setVisibility(expanded ? View.VISIBLE : View.GONE);
        binding.mealsList.removeAllViews();
        if (!expanded) {
            return;
        }
        if (meals.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.meals_empty);
            empty.setTextColor(binding.mealsCount.getCurrentTextColor());
            int padding = getResources().getDimensionPixelSize(R.dimen.meals_empty_padding);
            empty.setPadding(padding, 0, padding, padding / 2);
            binding.mealsList.addView(empty);
            return;
        }
        for (MealSummary meal : meals) {
            ItemSavedMealBinding row = ItemSavedMealBinding.inflate(getLayoutInflater(), binding.mealsList, true);
            row.name.setText(meal.name());
            row.detail.setText(meal.kcal() == null ? meal.itemsText()
                    : getString(R.string.meal_summary, meal.itemsText(), Formats.amount(meal.kcal(), NutrientUnit.KCAL)));
            row.edit.setContentDescription(getString(R.string.meal_edit, meal.name()));
            row.edit.setOnClickListener(v -> openMealEditor(MealEditorSheet.editMeal(meal.mealId())));
            row.add.setContentDescription(getString(R.string.meal_add_today, meal.name()));
            row.add.setOnClickListener(v -> viewModel.addMealAsIs(meal.mealId()));
        }
    }

    private void selectFirstSuggestion() {
        boolean sheetOpen = getChildFragmentManager().findFragmentByTag(QuantityInputSheet.TAG) != null;
        if (!sheetOpen && viewModel.selected().getValue() == null && suggestionAdapter.getItemCount() > 0) {
            viewModel.select(suggestionAdapter.getCurrentList().get(0));
        }
    }

    private void hideKeyboard() {
        if (binding == null) {
            return;
        }
        binding.foodInput.clearFocus();
        WindowCompat.getInsetsController(requireActivity().getWindow(), binding.getRoot())
                .hide(WindowInsetsCompat.Type.ime());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        sectionCards.clear();
        binding = null;
    }
}
