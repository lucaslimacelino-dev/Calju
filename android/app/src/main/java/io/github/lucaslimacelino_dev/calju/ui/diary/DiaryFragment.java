package io.github.lucaslimacelino_dev.calju.ui.diary;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.LinearLayout;
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
import io.github.lucaslimacelino_dev.calju.domain.FoodInputParser;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import io.github.lucaslimacelino_dev.calju.domain.Portion;
import io.github.lucaslimacelino_dev.calju.domain.PortionText;
import io.github.lucaslimacelino_dev.calju.ui.DateRequest;
import io.github.lucaslimacelino_dev.calju.ui.Dates;
import io.github.lucaslimacelino_dev.calju.ui.Formats;
import io.github.lucaslimacelino_dev.calju.ui.components.NumberAnimator;
import io.github.lucaslimacelino_dev.calju.ui.food.CustomFoodSheet;

public class DiaryFragment extends Fragment {

    /** Com mais sugestões que isso, a lista ganha altura fixa e rola dentro do cartão. */
    private static final int SUGGESTIONS_WITHOUT_SCROLL = 8;

    /** Fonte do sistema em 130% ou mais. */
    private static final float LARGE_FONT_SCALE = 1.3f;

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

        if (getResources().getConfiguration().fontScale >= LARGE_FONT_SCALE) {
            stackMacros();
        }
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
        viewModel.hiddenSuggestions().observe(getViewLifecycleOwner(), hidden -> showCreateOption());
        binding.moreFoods.setOnClickListener(v -> viewModel.showAllSuggestions());
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
                showCreateOption();
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
        binding.createFood.setOnClickListener(v -> {
            hideKeyboard();
            if (getChildFragmentManager().findFragmentByTag(CustomFoodSheet.TAG) == null) {
                CustomFoodSheet.newFood(capitalize(typedQuery()))
                        .show(getChildFragmentManager(), CustomFoodSheet.TAG);
            }
        });
        // Alimento criado aqui: limpa o campo e já abre a tela de quantidade com ele.
        getChildFragmentManager().setFragmentResultListener(CustomFoodSheet.RESULT_KEY, getViewLifecycleOwner(),
                (key, result) -> {
                    if (result.getBoolean(CustomFoodSheet.RESULT_CREATED)) {
                        binding.foodInput.setText("");
                        viewModel.clearSuggestions();
                        viewModel.selectFood(result.getString(CustomFoodSheet.RESULT_FOOD_ID));
                    }
                });
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

        showCaju(CajuMessages.forDay(state, viewModel.today()));
        showSections(OccasionSection.group(state.entries()));
    }

    private void showCaju(CajuMessages.Line line) {
        binding.caju.setImageResource(cajuDrawable(line.mood()));
        String message;
        switch (line.kind()) {
            case EMPTY_TODAY:
                message = getString(R.string.caju_empty_today);
                break;
            case EMPTY_PAST:
                message = getString(R.string.caju_empty_past);
                break;
            case PAST_DAY:
                message = getResources().getQuantityString(R.plurals.caju_past_day, line.count(), line.count());
                break;
            case NO_GOAL:
                message = getResources().getQuantityString(R.plurals.caju_no_goal, line.count(), line.count());
                break;
            case REMAINING:
                message = getString(R.string.caju_remaining, Formats.amount(line.kcal(), NutrientUnit.KCAL));
                break;
            case PROTEIN_DONE:
                message = getString(R.string.caju_protein_done);
                break;
            default:
                message = getString(R.string.caju_over_goal);
                break;
        }
        binding.cajuMessage.setText(message);
    }

    private static int cajuDrawable(CajuMessages.Mood mood) {
        switch (mood) {
            case CELEBRATING:
                return R.drawable.ic_caju_comemorando;
            case THINKING:
                return R.drawable.ic_caju_pensando;
            case SLEEPING:
                return R.drawable.ic_caju_dormindo;
            default:
                return R.drawable.ic_caju_feliz;
        }
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
            card.divider.setVisibility(occasion == Occasion.values()[Occasion.values().length - 1]
                    ? View.GONE : View.VISIBLE);
            sectionCards.put(occasion, card);
        }
    }

    private void showSections(List<OccasionSection> sections) {
        for (OccasionSection section : sections) {
            ItemOccasionSectionBinding card = sectionCards.get(section.occasion());
            card.kcal.setVisibility(section.isEmpty() ? View.GONE : View.VISIBLE);
            card.detail.setVisibility(section.isEmpty() ? View.VISIBLE : View.GONE);
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
        String grams = Formats.quantity(row.totalGrams(), row.unit());
        if (row.isSingleItem() && row.snapshot() != null && row.snapshot().portion().isPresent()) {
            Portion portion = row.snapshot().portion().get();
            BigDecimal count = PortionText.count(row.totalGrams(), portion.grams());
            if (count != null) {
                grams = Formats.count(count) + " " + PortionText.noun(portion.label(), count) + " · " + grams;
            }
        }
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
        binding.goalTitle.setText(goal == null ? R.string.summary_no_goal : R.string.summary_goal_title);
        binding.goalValue.setVisibility(goal == null ? View.GONE : View.VISIBLE);
        binding.goalConsumed.setText(goal == null ? getString(R.string.summary_no_goal_hint)
                : getString(R.string.summary_consumed, mark(kcal.complete(), R.string.value_at_least,
                value -> Formats.amount(value, NutrientUnit.KCAL)).apply(kcal.amount())));
        if (goal == null) {
            NumberAnimator.animate(binding.kcalValue, kcal.amount(), mark(kcal.complete(), R.string.value_at_least,
                    value -> Formats.amount(value, NutrientUnit.KCAL)));
            binding.kcalCaption.setText(R.string.kcal_consumed);
            binding.ring.setProgress(0f);
            binding.ringContainer.setContentDescription(getString(R.string.kcal_ring_description, consumed));
            return;
        }
        String target = Formats.amount(goal.kcal(), NutrientUnit.KCAL);
        binding.goalValue.setText(getString(R.string.summary_goal_value, target));
        BigDecimal remaining = goal.kcal().subtract(kcal.amount());
        boolean over = remaining.signum() < 0;
        Function<BigDecimal, String> format = mark(kcal.complete(),
                over ? R.string.value_at_least : R.string.value_at_most,
                value -> Formats.amount(value, NutrientUnit.KCAL));
        NumberAnimator.animate(binding.kcalValue, remaining.abs(), format);
        int caption = over ? R.string.kcal_over : R.string.kcal_remaining;
        binding.kcalCaption.setText(caption);
        binding.ring.setProgress(kcal.amount().floatValue() / goal.kcal().floatValue());
        // Valor final (o número na tela pode estar no meio da animação).
        binding.ringContainer.setContentDescription(getString(R.string.kcal_ring_description_goal, consumed, target)
                + ". " + format.apply(remaining.abs()) + " " + getString(caption));
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

    /**
     * Fonte grande: os três blocos de macros ficam um embaixo do outro, com a largura toda, para
     * que "Carboidrato" e "42,1 g" não quebrem no meio.
     */
    private void stackMacros() {
        binding.macros.setOrientation(LinearLayout.VERTICAL);
        int gap = getResources().getDimensionPixelSize(R.dimen.macro_stack_gap);
        for (int i = 0; i < binding.macros.getChildCount(); i++) {
            View card = binding.macros.getChildAt(i);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) card.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.weight = 0;
            params.topMargin = i == 0 ? 0 : gap;
            card.setLayoutParams(params);
        }
    }

    /** Total incompleto ganha um prefixo ("≥" ou "≤"): algum alimento não tem o valor medido. */
    private Function<BigDecimal, String> mark(boolean complete, int prefix, Function<BigDecimal, String> format) {
        return value -> complete ? format.apply(value) : getString(prefix, format.apply(value));
    }

    private void showSuggestions(List<Suggestion> suggestions) {
        suggestionAdapter.submitList(suggestions);
        ViewGroup.LayoutParams params = binding.suggestions.getLayoutParams();
        params.height = suggestions.size() > SUGGESTIONS_WITHOUT_SCROLL
                ? getResources().getDimensionPixelSize(R.dimen.suggestions_max_height)
                : ViewGroup.LayoutParams.WRAP_CONTENT;
        binding.suggestions.setLayoutParams(params);
        showCreateOption();
    }

    /**
     * Com texto digitado, o cartão mostra também "Criar alimento": mesmo sem nenhuma sugestão,
     * o usuário pode cadastrar o produto da embalagem que não está na TACO.
     */
    private void showCreateOption() {
        String query = typedQuery();
        boolean canCreate = query.length() >= SuggestionSearch.MIN_QUERY_LENGTH;
        binding.createFood.setVisibility(canCreate ? View.VISIBLE : View.GONE);
        binding.createFood.setText(getString(R.string.food_create, query));
        Integer hidden = viewModel.hiddenSuggestions().getValue();
        boolean more = canCreate && hidden != null && hidden > 0;
        binding.moreFoods.setVisibility(more ? View.VISIBLE : View.GONE);
        if (more) {
            binding.moreFoods.setText(getString(R.string.suggestion_more, hidden));
        }
        binding.suggestionsCard.setVisibility(canCreate || suggestionAdapter.getItemCount() > 0
                ? View.VISIBLE : View.GONE);
    }

    /** O nome digitado, sem números nem unidades ("granola 40 g" vira "granola"). */
    private String typedQuery() {
        Editable text = binding.foodInput.getText();
        return text == null ? "" : FoodInputParser.parse(text.toString()).query();
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(PT_BR) + text.substring(1);
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
        if (event.type() == DiaryEvent.Type.ADDED) {
            celebrate(snackbar);
        }
        snackbar.show();
    }

    /** Registro feito: o Caju comemora ao lado da mensagem do aviso. */
    private void celebrate(Snackbar snackbar) {
        TextView text = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        Drawable caju = ContextCompat.getDrawable(requireContext(), R.drawable.ic_caju_comemorando);
        if (text == null || caju == null) {
            return;
        }
        int size = getResources().getDimensionPixelSize(R.dimen.snackbar_caju);
        caju.setBounds(0, 0, size * 120 / 140, size);
        text.setCompoundDrawablesRelative(caju, null, null, null);
        text.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.snackbar_caju_padding));
        text.setGravity(Gravity.CENTER_VERTICAL);
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
        binding.mealsCount.setText(getResources().getQuantityString(R.plurals.meals_count, meals.size(), meals.size()));
        binding.mealsChevron.setRotation(expanded ? 180f : 0f);
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
