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

import io.github.lucaslimacelino_dev.calju.BuildConfig;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.FragmentProfileBinding;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

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
                Snackbar.make(binding.getRoot(), R.string.goals_saved, Snackbar.LENGTH_SHORT)
                        .setAnchorView(requireActivity().findViewById(R.id.bottom_navigation))
                        .show();
            }
        });
        binding.saveGoals.setOnClickListener(v -> save());
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
