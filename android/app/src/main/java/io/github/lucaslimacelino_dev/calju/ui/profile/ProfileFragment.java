package io.github.lucaslimacelino_dev.calju.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.BuildConfig;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.FragmentProfileBinding;
import io.github.lucaslimacelino_dev.calju.databinding.ItemCustomFoodBinding;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncEngine;
import io.github.lucaslimacelino_dev.calju.ui.Formats;
import io.github.lucaslimacelino_dev.calju.ui.account.AccountSheet;
import io.github.lucaslimacelino_dev.calju.ui.food.CustomFoodSheet;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private boolean filled;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        filled = savedInstanceState != null;
        viewModel = new ViewModelProvider(this,
                ProfileViewModel.factory(((CaLjuApp) requireActivity().getApplication()).container()))
                .get(ProfileViewModel.class);

        binding.version.setText(getString(R.string.about_version, BuildConfig.VERSION_NAME));
        viewModel.currentGoal().observe(getViewLifecycleOwner(), this::fill);
        viewModel.saved().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved)) {
                viewModel.savedMessageShown();
                message(getString(R.string.goals_saved)).show();
            }
        });
        binding.saveGoals.setOnClickListener(v -> save());

        viewModel.signedInEmail().observe(getViewLifecycleOwner(), this::showAccount);
        binding.syncButton.setOnClickListener(v -> viewModel.syncNow());
        viewModel.syncing().observe(getViewLifecycleOwner(),
                busy -> binding.syncButton.setEnabled(!Boolean.TRUE.equals(busy)));
        viewModel.syncResult().observe(getViewLifecycleOwner(), this::showSyncResult);
        getChildFragmentManager().setFragmentResultListener(AccountSheet.RESULT_KEY, getViewLifecycleOwner(),
                (key, result) -> {
                    String email = viewModel.signedInEmail().getValue();
                    if (email != null) {
                        message(getString(R.string.account_signed_in_message, email)).show();
                    }
                });

        viewModel.customFoods().observe(getViewLifecycleOwner(), this::showFoods);
        binding.newFood.setOnClickListener(v -> openFood(CustomFoodSheet.newFood("")));
        getChildFragmentManager().setFragmentResultListener(CustomFoodSheet.RESULT_KEY, getViewLifecycleOwner(),
                (key, result) -> {
                    String name = result.getString(CustomFoodSheet.RESULT_NAME);
                    if (result.getBoolean(CustomFoodSheet.RESULT_DELETED)) {
                        String foodId = result.getString(CustomFoodSheet.RESULT_FOOD_ID);
                        message(getString(R.string.food_deleted, name))
                                .setAction(R.string.undo, v -> viewModel.restoreFood(foodId))
                                .show();
                    } else {
                        message(getString(R.string.food_saved, name)).show();
                    }
                });
    }

    private void showSyncResult(SyncEngine.Result result) {
        if (result == null) {
            return;
        }
        viewModel.syncResultShown();
        String text;
        switch (result.outcome()) {
            case DONE:
                text = result.conflicts() > 0 ? getString(R.string.sync_done_conflicts, result.conflicts())
                        : getString(R.string.sync_done, result.sent(), result.received());
                break;
            case OFFLINE:
                text = getString(R.string.sync_offline);
                break;
            case SIGNED_OUT:
                text = getString(R.string.sync_signed_out);
                break;
            default:
                text = getString(R.string.sync_server_error);
                break;
        }
        message(text).show();
    }

    /** Sem conta: convite para entrar. Com conta: e-mail, "Sincronizar agora" e "Sair". */
    private void showAccount(String email) {
        binding.syncButton.setVisibility(email == null ? View.GONE : View.VISIBLE);
        if (email == null) {
            binding.accountStatus.setText(R.string.account_section_signed_out);
            binding.accountButton.setText(R.string.account_open);
            binding.accountButton.setOnClickListener(v -> {
                if (getChildFragmentManager().findFragmentByTag(AccountSheet.TAG) == null) {
                    new AccountSheet().show(getChildFragmentManager(), AccountSheet.TAG);
                }
            });
        } else {
            binding.accountStatus.setText(getString(R.string.account_section_signed_in, email));
            binding.accountButton.setText(R.string.account_sign_out);
            binding.accountButton.setOnClickListener(v -> {
                viewModel.signOut();
                message(getString(R.string.account_signed_out_message)).show();
            });
        }
    }

    private Snackbar message(String text) {
        return Snackbar.make(binding.getRoot(), text, Snackbar.LENGTH_LONG)
                .setAnchorView(requireActivity().findViewById(R.id.bottom_navigation));
    }

    private void openFood(CustomFoodSheet sheet) {
        if (getChildFragmentManager().findFragmentByTag(CustomFoodSheet.TAG) == null) {
            sheet.show(getChildFragmentManager(), CustomFoodSheet.TAG);
        }
    }

    private void showFoods(List<ProfileViewModel.FoodSummary> foods) {
        binding.myFoodsEmpty.setVisibility(foods.isEmpty() ? View.VISIBLE : View.GONE);
        binding.myFoodsList.removeAllViews();
        for (ProfileViewModel.FoodSummary food : foods) {
            ItemCustomFoodBinding row = ItemCustomFoodBinding.inflate(getLayoutInflater(), binding.myFoodsList, true);
            row.name.setText(food.name());
            String kcal = food.kcalPer100g() == null ? getString(R.string.value_unknown)
                    : Formats.amount(food.kcalPer100g(), NutrientUnit.KCAL);
            row.detail.setText(food.brand() == null ? getString(R.string.my_food_detail, kcal)
                    : getString(R.string.my_food_detail_brand, food.brand(), kcal));
            row.edit.setContentDescription(getString(R.string.my_food_edit, food.name()));
            row.edit.setOnClickListener(v -> openFood(CustomFoodSheet.edit(food.id())));
        }
    }

    /** Preenche os campos uma vez com a meta atual, sem apagar o que o usuário já digitou. */
    private void fill(Goal goal) {
        if (filled || goal == null) {
            return;
        }
        filled = true;
        set(binding.kcal, goal.kcal());
        set(binding.protein, goal.proteinG());
        set(binding.carbohydrate, goal.carbohydrateG());
        set(binding.fat, goal.fatG());
    }

    private static void set(TextInputEditText field, BigDecimal value) {
        field.setText(value == null ? "" : value.stripTrailingZeros().toPlainString().replace('.', ','));
    }

    private void save() {
        BigDecimal kcal = Formats.parsePositive(text(binding.kcal));
        binding.kcalLayout.setError(kcal == null ? getString(R.string.goals_kcal_required) : null);
        boolean valid = kcal != null;
        BigDecimal protein = optional(binding.proteinLayout, binding.protein);
        BigDecimal carbohydrate = optional(binding.carbohydrateLayout, binding.carbohydrate);
        BigDecimal fat = optional(binding.fatLayout, binding.fat);
        valid &= binding.proteinLayout.getError() == null
                && binding.carbohydrateLayout.getError() == null
                && binding.fatLayout.getError() == null;
        if (!valid) {
            return;
        }
        WindowCompat.getInsetsController(requireActivity().getWindow(), binding.getRoot())
                .hide(WindowInsetsCompat.Type.ime());
        viewModel.save(kcal, protein, carbohydrate, fat);
    }

    /** Campo opcional: vazio = sem meta; preenchido precisa ser um número positivo. */
    private BigDecimal optional(TextInputLayout layout, TextInputEditText field) {
        String value = text(field).trim();
        BigDecimal parsed = Formats.parsePositive(value);
        layout.setError(!value.isEmpty() && parsed == null ? getString(R.string.goals_invalid) : null);
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
