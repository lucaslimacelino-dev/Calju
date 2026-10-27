package io.github.lucaslimacelino_dev.calju.ui.diary;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.data.local.ProfileStore;
import io.github.lucaslimacelino_dev.calju.databinding.ItemMicronutrientBinding;
import io.github.lucaslimacelino_dev.calju.databinding.SheetMicronutrientsBinding;
import io.github.lucaslimacelino_dev.calju.domain.DailyValues;
import io.github.lucaslimacelino_dev.calju.domain.DailyValues.Reference;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;
import io.github.lucaslimacelino_dev.calju.ui.Formats;
import io.github.lucaslimacelino_dev.calju.ui.Motion;

/** Fibras, minerais e vitaminas do dia, em grupos, com a porcentagem da referência da pessoa. */
public class MicronutrientsSheet extends BottomSheetDialogFragment {

    public static final String TAG = "MicronutrientsSheet";
    private static final String STATE_EXPANDED = "expanded";

    private SheetMicronutrientsBinding binding;
    private ProfileStore profiles;
    private final Set<String> expanded = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetMicronutrientsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (savedInstanceState != null && savedInstanceState.getStringArrayList(STATE_EXPANDED) != null) {
            expanded.addAll(savedInstanceState.getStringArrayList(STATE_EXPANDED));
        }
        AppContainer container = ((CaLjuApp) requireActivity().getApplication()).container();
        profiles = container.profileStore;
        DiaryViewModel viewModel = new ViewModelProvider(requireParentFragment(), DiaryViewModel.factory(container))
                .get(DiaryViewModel.class);
        viewModel.dayState().observe(getViewLifecycleOwner(), this::render);
    }

    /** A lista é longa: abre inteira em vez de pela metade. */
    @Override
    public void onStart() {
        super.onStart();
        BottomSheetBehavior<?> behavior = ((BottomSheetDialog) requireDialog()).getBehavior();
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putStringArrayList(STATE_EXPANDED, new ArrayList<>(expanded));
    }

    private void render(DayState state) {
        if (state == null) {
            return;
        }
        for (LinearLayout group : new LinearLayout[]{binding.groupMinerals, binding.groupVitamins,
                binding.groupWatch}) {
            group.removeAllViews();
        }
        LayoutInflater inflater = getLayoutInflater();
        BigDecimal goalKcal = state.goal() == null ? null : state.goal().kcal();
        for (Reference reference : DailyValues.forPerson(profiles.load(), state.date(), goalKcal)) {
            LinearLayout group = switch (reference.group()) {
                case FIBER_AND_MINERALS -> binding.groupMinerals;
                case VITAMINS -> binding.groupVitamins;
                case WATCH -> binding.groupWatch;
            };
            ItemMicronutrientBinding row = ItemMicronutrientBinding.inflate(inflater, group, true);
            bind(row, reference, state.intakes());
        }
    }

    private void bind(ItemMicronutrientBinding row, Reference reference, List<Intake> intakes) {
        String name = getString(name(reference.code()));
        row.name.setText(name);
        row.info.setText(info(reference.code()));

        boolean anyKnown = intakes.isEmpty();
        for (Intake intake : intakes) {
            anyKnown |= intake.food().nutrient(reference.code()).isPresent();
        }
        String percentDescription = "";
        int progress = 0;
        if (!anyKnown) {
            row.value.setText(R.string.value_unknown);
            row.percent.setText("");
        } else {
            NutrientTotal total = NutritionCalculator.total(intakes, reference.code());
            String value = Formats.amount(total.amount(), reference.unit()) + " " + reference.unit().symbol();
            row.value.setText(total.complete() ? value : getString(R.string.value_at_least, value));
            Integer percent = DailyValues.percent(reference, total.amount());
            if (percent == null) {
                row.percent.setText("");
            } else {
                String text = getString(R.string.micronutrient_percent, percent);
                row.percent.setText(total.complete() ? text : getString(R.string.value_at_least, text));
                progress = Math.min(100, percent);
                percentDescription = getString(R.string.micronutrient_percent_description, percent);
            }
        }
        // Sem referência (colesterol): só a quantidade, sem barra.
        row.bar.setVisibility(reference.dailyValue() == null ? View.GONE : View.VISIBLE);
        row.bar.setProgressCompat(progress, true);
        row.getRoot().setContentDescription(name + ", " + row.value.getText()
                + (percentDescription.isEmpty() ? "" : ". " + percentDescription));

        showExpanded(row, expanded.contains(reference.code()), false);
        row.getRoot().setOnClickListener(v -> {
            boolean open = !expanded.remove(reference.code());
            if (open) {
                expanded.add(reference.code());
            }
            showExpanded(row, open, true);
        });
    }

    private void showExpanded(ItemMicronutrientBinding row, boolean open, boolean animate) {
        if (animate) {
            Motion.animateLayout((ViewGroup) binding.getRoot().getChildAt(0));
            Motion.rotateChevron(row.expand, open);
        } else {
            row.expand.setRotation(open ? 180f : 0f);
        }
        row.info.setVisibility(open ? View.VISIBLE : View.GONE);
        ViewCompat.replaceAccessibilityAction(row.getRoot(),
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                getString(open ? R.string.micronutrient_collapse : R.string.micronutrient_expand), null);
    }

    @StringRes
    private static int name(String code) {
        return switch (code) {
            case "FIBTG" -> R.string.nutrient_FIBTG;
            case "CA" -> R.string.nutrient_CA;
            case "FE" -> R.string.nutrient_FE;
            case "MG" -> R.string.nutrient_MG;
            case "K" -> R.string.nutrient_K;
            case "ZN" -> R.string.nutrient_ZN;
            case "P" -> R.string.nutrient_P;
            case "CU" -> R.string.nutrient_CU;
            case "MN" -> R.string.nutrient_MN;
            case "VITC" -> R.string.nutrient_VITC;
            case "VITA_RAE" -> R.string.nutrient_VITA_RAE;
            case "VITB12" -> R.string.nutrient_VITB12;
            case "THIA" -> R.string.nutrient_THIA;
            case "RIBF" -> R.string.nutrient_RIBF;
            case "VITB6A" -> R.string.nutrient_VITB6A;
            case "NIA" -> R.string.nutrient_NIA;
            case "NA" -> R.string.nutrient_NA;
            case "CHOLE" -> R.string.nutrient_CHOLE;
            default -> throw new IllegalArgumentException(code);
        };
    }

    @StringRes
    private static int info(String code) {
        return switch (code) {
            case "FIBTG" -> R.string.nutrient_info_FIBTG;
            case "CA" -> R.string.nutrient_info_CA;
            case "FE" -> R.string.nutrient_info_FE;
            case "MG" -> R.string.nutrient_info_MG;
            case "K" -> R.string.nutrient_info_K;
            case "ZN" -> R.string.nutrient_info_ZN;
            case "P" -> R.string.nutrient_info_P;
            case "CU" -> R.string.nutrient_info_CU;
            case "MN" -> R.string.nutrient_info_MN;
            case "VITC" -> R.string.nutrient_info_VITC;
            case "VITA_RAE" -> R.string.nutrient_info_VITA_RAE;
            case "VITB12" -> R.string.nutrient_info_VITB12;
            case "THIA" -> R.string.nutrient_info_THIA;
            case "RIBF" -> R.string.nutrient_info_RIBF;
            case "VITB6A" -> R.string.nutrient_info_VITB6A;
            case "NIA" -> R.string.nutrient_info_NIA;
            case "NA" -> R.string.nutrient_info_NA;
            case "CHOLE" -> R.string.nutrient_info_CHOLE;
            default -> throw new IllegalArgumentException(code);
        };
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
