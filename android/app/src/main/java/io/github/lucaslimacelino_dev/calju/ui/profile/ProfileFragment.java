package io.github.lucaslimacelino_dev.calju.ui.profile;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.BuildConfig;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.FragmentProfileBinding;
import io.github.lucaslimacelino_dev.calju.databinding.ItemCustomFoodBinding;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Sex;
import io.github.lucaslimacelino_dev.calju.domain.UserProfile;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.data.sync.AccountBinding;
import io.github.lucaslimacelino_dev.calju.data.sync.ConflictResolver;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncEngine;
import io.github.lucaslimacelino_dev.calju.ui.Dates;
import io.github.lucaslimacelino_dev.calju.ui.Formats;
import io.github.lucaslimacelino_dev.calju.ui.Motion;
import io.github.lucaslimacelino_dev.calju.ui.account.AccountSheet;
import io.github.lucaslimacelino_dev.calju.ui.diary.Occasions;
import io.github.lucaslimacelino_dev.calju.ui.food.CustomFoodSheet;
import io.github.lucaslimacelino_dev.calju.ui.goal.GoalQuizActivity;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private static final String STATE_ABOUT_OPEN = "about_open";

    private boolean filled;
    private boolean aboutOpen;
    private final ActivityResultLauncher<Intent> quiz = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    showCalculated(result.getData());
                }
            });

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
        showAbout(savedInstanceState != null && savedInstanceState.getBoolean(STATE_ABOUT_OPEN), false);
        binding.aboutHeader.setOnClickListener(v -> showAbout(!aboutOpen, true));
        viewModel.currentGoal().observe(getViewLifecycleOwner(), this::fill);
        viewModel.saved().observe(getViewLifecycleOwner(), saved -> {
            if (Boolean.TRUE.equals(saved)) {
                viewModel.savedMessageShown();
                message(getString(R.string.goals_saved)).show();
            }
        });
        binding.saveGoals.setOnClickListener(v -> save());
        binding.calculateGoals.setOnClickListener(v -> quiz.launch(GoalQuizActivity.fromProfile(requireContext())));
        binding.deleteProfileData.setOnClickListener(v -> confirmDeleteProfile());

        binding.accountCard.setVisibility(BuildConfig.ACCOUNT_ENABLED ? View.VISIBLE : View.GONE);
        binding.offlinePrivacy.setVisibility(BuildConfig.ACCOUNT_ENABLED ? View.GONE : View.VISIBLE);
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
                        viewModel.afterSignIn(email);
                    }
                });
        viewModel.bindingDecision().observe(getViewLifecycleOwner(), this::askBinding);
        viewModel.signOutPending().observe(getViewLifecycleOwner(), this::askSignOut);

        viewModel.conflictCount().observe(getViewLifecycleOwner(), count -> showSyncIssues());
        viewModel.failedCount().observe(getViewLifecycleOwner(), count -> showSyncIssues());
        binding.resolveButton.setOnClickListener(v -> viewModel.resolveNext());
        binding.retryButton.setOnClickListener(v -> viewModel.retryFailed());
        viewModel.conflictChoice().observe(getViewLifecycleOwner(), this::askConflict);
        viewModel.conflictMessage().observe(getViewLifecycleOwner(), text -> {
            if (text != null) {
                viewModel.conflictMessageShown();
                message(getString(text)).show();
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

    /** Primeira conta com dados no aparelho, ou dados de outra conta: a pessoa decide. */
    private void askBinding(AccountBinding.Decision decision) {
        String email = viewModel.signedInEmail().getValue();
        if (decision == null || email == null) {
            return;
        }
        MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(requireContext()).setCancelable(false);
        if (decision == AccountBinding.Decision.ASK_UPLOAD) {
            dialog.setTitle(R.string.binding_upload_title)
                    .setMessage(getString(R.string.binding_upload_message, email))
                    .setPositiveButton(R.string.binding_upload_yes, (d, w) -> viewModel.uploadLocalData(email))
                    .setNegativeButton(R.string.binding_upload_no, (d, w) -> viewModel.replaceLocalData(email));
        } else {
            dialog.setTitle(R.string.binding_other_title)
                    .setMessage(getString(R.string.binding_other_message, viewModel.boundEmail(), email))
                    .setPositiveButton(R.string.binding_other_yes, (d, w) -> viewModel.replaceLocalData(email))
                    .setNegativeButton(R.string.cancel, (d, w) -> viewModel.cancelSignIn());
        }
        dialog.show();
    }

    /** Conflitos para decidir e itens recusados pelo servidor, com os botões de cada caso. */
    private void showSyncIssues() {
        int conflicts = valueOf(viewModel.conflictCount().getValue());
        int failed = valueOf(viewModel.failedCount().getValue());
        StringBuilder text = new StringBuilder();
        if (conflicts > 0) {
            text.append(getResources().getQuantityString(R.plurals.sync_conflict_count, conflicts, conflicts));
        }
        if (failed > 0) {
            if (text.length() > 0) {
                text.append('\n');
            }
            text.append(getResources().getQuantityString(R.plurals.sync_failed_count, failed, failed));
        }
        boolean any = conflicts > 0 || failed > 0;
        binding.syncIssues.setVisibility(any ? View.VISIBLE : View.GONE);
        binding.syncIssues.setText(text);
        binding.syncIssueButtons.setVisibility(any ? View.VISIBLE : View.GONE);
        binding.resolveButton.setVisibility(conflicts > 0 ? View.VISIBLE : View.GONE);
        binding.retryButton.setVisibility(failed > 0 ? View.VISIBLE : View.GONE);
    }

    private static int valueOf(Integer count) {
        return count == null ? 0 : count;
    }

    /** Um conflito por vez: a versão deste celular e a da conta, e a pessoa escolhe qual fica. */
    private void askConflict(ConflictResolver.Choice choice) {
        if (choice == null) {
            return;
        }
        String title = choice.kind() == ConflictResolver.Kind.ENTRY
                ? getString(R.string.conflict_entry_title, getString(Occasions.label(choice.occasion())),
                Dates.full(choice.date()))
                : getString(R.string.conflict_goal_title, Dates.full(choice.date()));
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(title)
                .setMessage(getString(R.string.conflict_message, describe(choice.local()),
                        describe(choice.remote())))
                .setPositiveButton(R.string.conflict_keep_local, (d, w) -> viewModel.choose(choice, true))
                .setNegativeButton(R.string.conflict_use_server, (d, w) -> viewModel.choose(choice, false))
                .setNeutralButton(R.string.conflict_later, (d, w) -> viewModel.chooseLater())
                .setOnCancelListener(d -> viewModel.chooseLater())
                .show();
    }

    private String describe(ConflictResolver.Side side) {
        if (side.missing()) {
            return getString(R.string.conflict_side_missing);
        }
        if (side.deleted()) {
            return getString(R.string.conflict_side_deleted);
        }
        String kcal = Formats.amount(side.kcal(), NutrientUnit.KCAL);
        if (side.items().isEmpty()) {
            return getString(R.string.conflict_side_goal, kcal, optionalGrams(side.proteinG()),
                    optionalGrams(side.carbsG()), optionalGrams(side.fatG()));
        }
        StringBuilder items = new StringBuilder();
        for (ConflictResolver.Item item : side.items()) {
            if (items.length() > 0) {
                items.append(", ");
            }
            items.append(item.name()).append(' ').append(Formats.quantity(item.grams(), item.unit()));
        }
        return getString(R.string.conflict_side_entry, items, kcal);
    }

    private static String optionalGrams(BigDecimal grams) {
        return grams == null ? "—" : Formats.grams(grams);
    }

    /** Sair: manter ou apagar os dados deste celular (avisando se há algo ainda não enviado). */
    private void askSignOut(Integer pending) {
        if (pending == null) {
            return;
        }
        String text = getString(R.string.sign_out_message);
        if (pending > 0) {
            text += "\n\n" + getResources().getQuantityString(R.plurals.sign_out_pending, pending, pending);
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.sign_out_title)
                .setMessage(text)
                .setPositiveButton(R.string.sign_out_keep, (d, w) -> signOut(true))
                .setNegativeButton(R.string.sign_out_wipe, (d, w) -> signOut(false))
                .setNeutralButton(R.string.cancel, (d, w) -> viewModel.signOutCancelled())
                .setOnCancelListener(d -> viewModel.signOutCancelled())
                .show();
    }

    private void signOut(boolean keepData) {
        viewModel.signOut(keepData);
        message(getString(R.string.account_signed_out_message)).show();
    }

    private void showSyncResult(SyncEngine.Result result) {
        if (result == null) {
            return;
        }
        viewModel.syncResultShown();
        String text;
        switch (result.outcome()) {
            case DONE:
                text = result.conflicts() > 0
                        ? getResources().getQuantityString(R.plurals.sync_done_conflicts, result.conflicts(),
                        result.conflicts())
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
            binding.accountButton.setOnClickListener(v -> viewModel.prepareSignOut());
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
            String unit = food.unit().symbol();
            row.detail.setText(food.brand() == null ? getString(R.string.my_food_detail, kcal, unit)
                    : getString(R.string.my_food_detail_brand, food.brand(), kcal, unit));
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

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_ABOUT_OPEN, aboutOpen);
    }

    @Override
    public void onResume() {
        super.onResume();
        showProfile();
    }

    /** Resumo das respostas do questionário, se houver. */
    private void showProfile() {
        UserProfile profile = container().profileStore.load();
        boolean has = profile != null;
        binding.deleteProfileData.setVisibility(has ? View.VISIBLE : View.GONE);
        binding.calculateGoals.setText(has ? R.string.profile_redo_quiz : R.string.goals_calculate);
        if (!has) {
            binding.profileSummary.setText(R.string.profile_no_data);
            return;
        }
        String body = getString(R.string.profile_data_summary,
                getString(profile.sex() == Sex.MALE ? R.string.profile_sex_male : R.string.profile_sex_female),
                profile.age(LocalDate.now(container().clock)), Formats.count(profile.heightCm()),
                Formats.count(profile.weightKg()));
        String plan = getString(switch (profile.objective()) {
            case LOSE -> R.string.profile_objective_lose;
            case KEEP -> R.string.profile_objective_keep;
            case GAIN -> R.string.profile_objective_gain;
        });
        if (profile.activity() != null) {
            plan = getString(R.string.profile_plan_with_activity, plan, getString(switch (profile.activity()) {
                case LIGHT -> R.string.profile_activity_light;
                case ACTIVE -> R.string.profile_activity_active;
                case VIGOROUS -> R.string.profile_activity_vigorous;
            }));
        }
        binding.profileSummary.setText(body + "\n" + plan);
    }

    /** "Sobre" e as fontes ficam recolhidos: abrem ao tocar no cabeçalho. */
    private void showAbout(boolean open, boolean animate) {
        aboutOpen = open;
        if (animate) {
            Motion.animateLayout(binding.profileContent);
            Motion.rotateChevron(binding.aboutExpand, open);
        } else {
            binding.aboutExpand.setRotation(open ? 180f : 0f);
        }
        binding.aboutContent.setVisibility(open ? View.VISIBLE : View.GONE);
        ViewCompat.replaceAccessibilityAction(binding.aboutHeader,
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                getString(open ? R.string.about_collapse : R.string.about_expand), null);
    }

    private void confirmDeleteProfile() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.profile_delete_title)
                .setMessage(R.string.profile_delete_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.profile_delete_confirm, (dialog, which) -> {
                    container().profileStore.clear();
                    showProfile();
                    message(getString(R.string.profile_data_deleted)).show();
                })
                .show();
    }

    private AppContainer container() {
        return ((CaLjuApp) requireActivity().getApplication()).container();
    }

    /** A meta já foi salva pelo questionário; aqui só atualiza os campos. */
    private void showCalculated(Intent data) {
        filled = true;
        int kcal = data.getIntExtra(GoalQuizActivity.EXTRA_KCAL, 0);
        set(binding.kcal, BigDecimal.valueOf(kcal));
        set(binding.protein, BigDecimal.valueOf(data.getIntExtra(GoalQuizActivity.EXTRA_PROTEIN, 0)));
        set(binding.carbohydrate, BigDecimal.valueOf(data.getIntExtra(GoalQuizActivity.EXTRA_CARBOHYDRATE, 0)));
        set(binding.fat, BigDecimal.valueOf(data.getIntExtra(GoalQuizActivity.EXTRA_FAT, 0)));
        message(getString(R.string.quiz_goal_saved, Formats.count(BigDecimal.valueOf(kcal)))).show();
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
