package io.github.lucaslimacelino_dev.calju.ui.goal;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.MainActivity;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.data.repository.GoalRepository;
import io.github.lucaslimacelino_dev.calju.databinding.ActivityGoalQuizBinding;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Activity;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Answers;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Objective;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Pace;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Plan;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Sex;
import io.github.lucaslimacelino_dev.calju.domain.UserProfile;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

/**
 * Questionário que sugere a meta diária. Na primeira abertura é obrigatório. Ao terminar, as
 * respostas ficam guardadas no aparelho ({@link UserProfile}, usado também nos micronutrientes) e
 * a meta vai para o banco, se a pessoa aceitar.
 */
public class GoalQuizActivity extends AppCompatActivity {

    public static final String EXTRA_KCAL = "kcal";
    public static final String EXTRA_PROTEIN = "protein";
    public static final String EXTRA_CARBOHYDRATE = "carbohydrate";
    public static final String EXTRA_FAT = "fat";

    private static final String EXTRA_ONBOARDING = "onboarding";
    private static final String STATE_STEP = "step";

    private static final int OBJECTIVE = 0;
    private static final int PACE = 1;
    private static final int BODY = 2;
    private static final int ACTIVITY = 3;
    private static final int HEALTH = 4;
    private static final int RESULT = 5;
    /** Fim sem cálculo: situação de saúde especial ou idade fora da faixa da equação. */
    private static final int PROFESSIONAL = 6;
    private static final int AGE_OUT = 7;

    private static final int MIN_HEIGHT_CM = 100;
    private static final int MAX_HEIGHT_CM = 250;
    private static final int MIN_WEIGHT_KG = 30;
    private static final int MAX_WEIGHT_KG = 300;

    private ActivityGoalQuizBinding binding;
    private boolean onboarding;
    private int step;
    private Plan plan;

    /** Logo depois das boas-vindas (ou sem respostas guardadas): o fim do questionário leva ao Diário. */
    public static Intent onboarding(Context context) {
        return new Intent(context, GoalQuizActivity.class).putExtra(EXTRA_ONBOARDING, true);
    }

    /** A partir do Perfil: devolve a meta salva nos extras do resultado. */
    public static Intent fromProfile(Context context) {
        return new Intent(context, GoalQuizActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityGoalQuizBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.quizRoot, (root, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            root.setPadding(root.getPaddingLeft(), bars.top, root.getPaddingRight(), bars.bottom);
            return insets;
        });

        onboarding = getIntent().getBooleanExtra(EXTRA_ONBOARDING, false);
        step = savedInstanceState == null ? OBJECTIVE : savedInstanceState.getInt(STATE_STEP);
        for (MaterialButtonToggleGroup group : List.of(binding.objectiveGroup, binding.paceGroup,
                binding.activityGroup, binding.healthGroup)) {
            for (int i = 0; i < group.getChildCount(); i++) {
                showTwoLines((MaterialButton) group.getChildAt(i));
            }
        }
        binding.back.setOnClickListener(v -> back());
        binding.next.setOnClickListener(v -> next());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (step == OBJECTIVE) {
                    // Na primeira abertura não há como pular: voltar fecha o app sem abrir o Diário.
                    finish();
                } else if (step == PROFESSIONAL || step == AGE_OUT) {
                    finishWithProfile();
                } else {
                    back();
                }
            }
        });
        if (savedInstanceState == null) {
            showStep();
        }
    }

    /** Os botões e campos recuperam o que foi escolhido só depois do onCreate. */
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        showStep();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP, step);
    }

    /** O botão do Material mostra uma linha só; nas opções, a 1ª linha é o título e o resto explica. */
    private static void showTwoLines(MaterialButton button) {
        button.setSingleLine(false);
        button.setMaxLines(5);
        String text = button.getText().toString();
        int lineBreak = text.indexOf('\n');
        if (lineBreak < 0) {
            return;
        }
        SpannableString styled = new SpannableString(text);
        styled.setSpan(new StyleSpan(Typeface.BOLD), 0, lineBreak, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        styled.setSpan(new RelativeSizeSpan(0.85f), lineBreak + 1, text.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        button.setText(styled);
    }

    private List<Integer> questions() {
        List<Integer> steps = new ArrayList<>();
        steps.add(OBJECTIVE);
        if (objective() == Objective.LOSE) {
            steps.add(PACE);
        }
        steps.add(BODY);
        steps.add(ACTIVITY);
        steps.add(HEALTH);
        return steps;
    }

    private void next() {
        switch (step) {
            case OBJECTIVE -> {
                if (require(binding.objectiveGroup)) {
                    goTo(objective() == Objective.LOSE ? PACE : BODY);
                }
            }
            case PACE -> {
                if (require(binding.paceGroup)) {
                    goTo(BODY);
                }
            }
            case BODY -> checkBody();
            case ACTIVITY -> {
                if (require(binding.activityGroup)) {
                    goTo(HEALTH);
                }
            }
            case HEALTH -> {
                if (require(binding.healthGroup)) {
                    goTo(binding.healthGroup.getCheckedButtonId() == R.id.health_yes ? PROFESSIONAL : RESULT);
                }
            }
            case RESULT -> save();
            default -> finishWithProfile();
        }
    }

    private void back() {
        int index = questions().indexOf(step);
        if (step == RESULT || step == PROFESSIONAL) {
            goTo(HEALTH);
        } else if (step == AGE_OUT) {
            goTo(BODY);
        } else if (index > 0) {
            goTo(questions().get(index - 1));
        }
    }

    private void goTo(int newStep) {
        step = newStep;
        showStep();
    }

    private void showStep() {
        binding.objectiveGroup.setVisibility(step == OBJECTIVE ? View.VISIBLE : View.GONE);
        binding.paceGroup.setVisibility(step == PACE ? View.VISIBLE : View.GONE);
        binding.bodyStep.setVisibility(step == BODY ? View.VISIBLE : View.GONE);
        binding.activityGroup.setVisibility(step == ACTIVITY ? View.VISIBLE : View.GONE);
        binding.healthGroup.setVisibility(step == HEALTH ? View.VISIBLE : View.GONE);
        binding.resultCard.setVisibility(step == RESULT ? View.VISIBLE : View.GONE);

        List<Integer> questions = questions();
        int index = questions.indexOf(step);
        binding.stepIndicator.setText(index < 0 ? "" : getString(R.string.quiz_step, index + 1, questions.size()));
        binding.back.setVisibility(step == OBJECTIVE ? View.INVISIBLE : View.VISIBLE);
        binding.caju.setImageResource(switch (step) {
            case RESULT -> R.drawable.ic_caju_comemorando;
            case BODY, ACTIVITY, HEALTH -> R.drawable.ic_caju_pensando;
            default -> R.drawable.ic_caju_feliz;
        });
        binding.question.setText(switch (step) {
            case OBJECTIVE -> R.string.quiz_q_objective;
            case PACE -> R.string.quiz_q_pace;
            case BODY -> R.string.quiz_q_body;
            case ACTIVITY -> R.string.quiz_q_activity;
            case HEALTH -> R.string.quiz_q_health;
            case RESULT -> R.string.quiz_q_result;
            case PROFESSIONAL -> R.string.quiz_q_professional;
            default -> R.string.quiz_q_age_out;
        });
        binding.next.setEnabled(true);
        if (step == RESULT) {
            showResult();
            binding.next.setText(R.string.quiz_use_goal);
        } else if (step == PROFESSIONAL || step == AGE_OUT) {
            binding.next.setText(onboarding ? R.string.quiz_go_app : R.string.quiz_close);
        } else {
            binding.next.setText(R.string.quiz_next);
        }
    }

    private boolean require(MaterialButtonToggleGroup group) {
        if (group.getCheckedButtonId() == View.NO_ID) {
            Snackbar.make(binding.getRoot(), R.string.quiz_choose, Snackbar.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private void checkBody() {
        if (binding.sexGroup.getCheckedButtonId() == View.NO_ID) {
            Snackbar.make(binding.getRoot(), R.string.quiz_choose_sex, Snackbar.LENGTH_SHORT).show();
            return;
        }
        Integer age = whole(binding.age);
        binding.ageLayout.setError(age == null || age <= 0 ? getString(R.string.quiz_invalid_age) : null);
        BigDecimal height = inRange(binding.height, binding.heightLayout, MIN_HEIGHT_CM, MAX_HEIGHT_CM,
                R.string.quiz_invalid_height);
        BigDecimal weight = inRange(binding.weight, binding.weightLayout, MIN_WEIGHT_KG, MAX_WEIGHT_KG,
                R.string.quiz_invalid_weight);
        if (age == null || age <= 0 || height == null || weight == null) {
            return;
        }
        goTo(age < GoalCalculator.MIN_AGE || age > GoalCalculator.MAX_AGE ? AGE_OUT : ACTIVITY);
    }

    private BigDecimal inRange(TextInputEditText field, TextInputLayout layout, int min, int max, int error) {
        BigDecimal value = Formats.parsePositive(text(field));
        boolean valid = value != null && value.compareTo(BigDecimal.valueOf(min)) >= 0
                && value.compareTo(BigDecimal.valueOf(max)) <= 0;
        layout.setError(valid ? null : getString(error));
        return valid ? value : null;
    }

    private static Integer whole(TextInputEditText field) {
        try {
            return Integer.valueOf(text(field).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    private Objective objective() {
        int id = binding.objectiveGroup.getCheckedButtonId();
        if (id == R.id.objective_lose) {
            return Objective.LOSE;
        }
        return id == R.id.objective_gain ? Objective.GAIN : Objective.KEEP;
    }

    private Answers answers() {
        Objective objective = objective();
        Pace pace = objective != Objective.LOSE ? null
                : binding.paceGroup.getCheckedButtonId() == R.id.pace_moderate ? Pace.MODERATE : Pace.SLOW;
        Sex sex = binding.sexGroup.getCheckedButtonId() == R.id.sex_male ? Sex.MALE : Sex.FEMALE;
        int activityId = binding.activityGroup.getCheckedButtonId();
        Activity activity = activityId == R.id.activity_vigorous ? Activity.VIGOROUS
                : activityId == R.id.activity_active ? Activity.ACTIVE : Activity.LIGHT;
        return new Answers(objective, pace, sex, whole(binding.age),
                Formats.parsePositive(text(binding.height)), Formats.parsePositive(text(binding.weight)), activity);
    }

    private void showResult() {
        Answers answers = answers();
        plan = GoalCalculator.plan(answers);
        binding.resultKcal.setText(getString(R.string.quiz_result_kcal, kcal(plan.targetKcal())));
        binding.resultMacros.setText(getString(R.string.quiz_result_macros, plan.proteinG(), plan.carbohydrateG(),
                plan.fatG()));

        StringBuilder details = new StringBuilder(getString(R.string.quiz_result_resting,
                kcal(plan.restingKcal()), kcal(plan.dailyKcal())));
        int objective = switch (answers.objective()) {
            case LOSE -> answers.pace() == Pace.MODERATE ? R.string.quiz_result_lose_moderate
                    : R.string.quiz_result_lose_slow;
            case KEEP -> R.string.quiz_result_keep;
            case GAIN -> R.string.quiz_result_gain;
        };
        details.append("\n\n").append(getString(objective));
        if (plan.limitedByResting()) {
            details.append(' ').append(getString(R.string.quiz_result_limited));
        }
        details.append("\n\n").append(getString(R.string.quiz_result_macros_note))
                .append("\n\n").append(getString(R.string.quiz_result_estimate));
        binding.resultDetails.setText(details);
    }

    private static String kcal(int value) {
        return Formats.count(BigDecimal.valueOf(value));
    }

    /** Situação especial ou idade fora da faixa: guarda as respostas (micros pela ANVISA) e sai. */
    private void finishWithProfile() {
        container().profileStore.save(profile());
        leave();
    }

    /**
     * Respostas até onde o questionário foi. Na idade fora da faixa ele para antes da rotina e da
     * pergunta de saúde.
     */
    private UserProfile profile() {
        Objective objective = objective();
        Pace pace = objective != Objective.LOSE ? null
                : binding.paceGroup.getCheckedButtonId() == R.id.pace_moderate ? Pace.MODERATE : Pace.SLOW;
        Sex sex = binding.sexGroup.getCheckedButtonId() == R.id.sex_male ? Sex.MALE : Sex.FEMALE;
        boolean reachedActivity = step != AGE_OUT;
        Activity activity = reachedActivity ? answers().activity() : null;
        boolean special = step == PROFESSIONAL;
        return UserProfile.fromAge(sex, whole(binding.age), container().goalRepository.today(),
                Formats.parsePositive(text(binding.height)), Formats.parsePositive(text(binding.weight)),
                activity, objective, pace, special);
    }

    private AppContainer container() {
        return ((CaLjuApp) getApplication()).container();
    }

    private void save() {
        Plan chosen = plan;
        binding.next.setEnabled(false);
        AppContainer container = container();
        container.profileStore.save(profile());
        GoalRepository goals = container.goalRepository;
        container.databaseExecutor.execute(() -> {
            goals.save(chosen.toGoal(goals.today()));
            runOnUiThread(() -> {
                if (onboarding) {
                    leave();
                } else {
                    setResult(RESULT_OK, new Intent()
                            .putExtra(EXTRA_KCAL, chosen.targetKcal())
                            .putExtra(EXTRA_PROTEIN, chosen.proteinG())
                            .putExtra(EXTRA_CARBOHYDRATE, chosen.carbohydrateG())
                            .putExtra(EXTRA_FAT, chosen.fatG()));
                    finish();
                }
            });
        });
    }

    private void leave() {
        if (onboarding) {
            startActivity(new Intent(this, MainActivity.class));
        }
        finish();
    }
}
