package io.github.lucaslimacelino_dev.calju.ui;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.MainActivity;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Activity;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Sex;
import io.github.lucaslimacelino_dev.calju.domain.UserProfile;

/** Questionário da meta aberto pelo Perfil. Pessoa de exemplo: mulher, 30 anos, 165 cm, 60 kg. */
@RunWith(AndroidJUnit4.class)
public class GoalQuizTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private CaLjuApp app;

    @Before
    public void clearGoals() {
        app = ApplicationProvider.getApplicationContext();
        app.container().database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM nutrition_goals");
        app.container().profileStore.clear();
    }

    private void openQuiz() {
        onView(withId(R.id.profileFragment)).perform(click());
        Waits.scrollAndTap(withId(R.id.calculate_goals));
        Waits.untilDisplayed(withText(R.string.quiz_q_objective));
    }

    private void answerBody(String age) {
        onView(withId(R.id.sex_female)).perform(click());
        onView(withId(R.id.age)).perform(replaceText(age));
        onView(withId(R.id.height)).perform(replaceText("165"));
        onView(withId(R.id.weight)).perform(replaceText("60"));
        closeSoftKeyboard();
        onView(withId(R.id.next)).perform(click());
    }

    @Test
    public void manterPesoSugereOGastoDoDiaESalvaAMeta() {
        openQuiz();
        onView(withText("1 de 4")).check(matches(isDisplayed()));
        onView(withId(R.id.objective_keep)).perform(click());
        onView(withId(R.id.next)).perform(click());

        onView(withText(R.string.quiz_q_body)).check(matches(isDisplayed()));
        answerBody("30");

        onView(withText(R.string.quiz_q_activity)).check(matches(isDisplayed()));
        onView(withId(R.id.activity_light)).perform(click());
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.health_no)).perform(click());
        onView(withId(R.id.next)).perform(click());

        // Repouso 1.320 kcal × 1,55 = 2.046 → 2.050; 20/50/30% = 103 g, 256 g e 68 g.
        onView(withId(R.id.result_kcal)).check(matches(withText("2.050 kcal por dia")));
        onView(withId(R.id.result_macros))
                .check(matches(withText("Proteína 103 g · Carboidrato 256 g · Gordura 68 g")));
        onView(withId(R.id.next)).perform(click());

        Waits.untilDisplayed(withText("Meta salva: 2.050 kcal por dia"));
        onView(withId(R.id.kcal)).perform(scrollTo()).check(matches(withText("2050")));
        List<Goal> goals = app.container().goalRepository.all();
        assertEquals(1, goals.size());
        assertEquals(0, new BigDecimal("2050").compareTo(goals.get(0).kcal()));
        assertEquals(0, new BigDecimal("103").compareTo(goals.get(0).proteinG()));

        UserProfile profile = app.container().profileStore.load();
        assertEquals(Sex.FEMALE, profile.sex());
        assertEquals(30, profile.age(LocalDate.now(app.container().clock)));
        assertEquals(Activity.LIGHT, profile.activity());
        assertFalse(profile.specialCondition());
        onView(withId(R.id.profile_summary)).perform(scrollTo())
                .check(matches(withText("Seus dados: mulher, 30 anos, 165 cm, 60 kg")));
    }

    @Test
    public void perderPesoPerguntaORitmo() {
        openQuiz();
        onView(withId(R.id.objective_lose)).perform(click());
        onView(withId(R.id.next)).perform(click());

        onView(withText(R.string.quiz_q_pace)).check(matches(isDisplayed()));
        onView(withText("2 de 5")).check(matches(isDisplayed()));
        onView(withId(R.id.next)).perform(click());
        onView(withText(R.string.quiz_choose)).check(matches(isDisplayed()));
    }

    @Test
    public void situacaoEspecialNaoCalculaENaoSalva() {
        openQuiz();
        onView(withId(R.id.objective_keep)).perform(click());
        onView(withId(R.id.next)).perform(click());
        answerBody("30");
        onView(withId(R.id.activity_light)).perform(click());
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.health_yes)).perform(click());
        onView(withId(R.id.next)).perform(click());

        onView(withText(R.string.quiz_q_professional)).check(matches(isDisplayed()));
        onView(withId(R.id.next)).perform(click());

        Waits.untilDisplayed(withId(R.id.calculate_goals));
        assertTrue(app.container().goalRepository.all().isEmpty());
        assertTrue(app.container().profileStore.load().specialCondition());
    }

    @Test
    public void idadeForaDaFaixaRecomendaProfissional() {
        openQuiz();
        onView(withId(R.id.objective_keep)).perform(click());
        onView(withId(R.id.next)).perform(click());
        answerBody("16");

        onView(withText(R.string.quiz_q_age_out)).check(matches(isDisplayed()));
    }

    @Test
    public void alturaImpossivelMostraErro() {
        openQuiz();
        onView(withId(R.id.objective_keep)).perform(click());
        onView(withId(R.id.next)).perform(click());
        onView(withId(R.id.sex_female)).perform(click());
        onView(withId(R.id.age)).perform(replaceText("30"));
        onView(withId(R.id.height)).perform(replaceText("17"));
        onView(withId(R.id.weight)).perform(replaceText("60"));
        closeSoftKeyboard();
        onView(withId(R.id.next)).perform(click());

        onView(withText(R.string.quiz_invalid_height)).check(matches(isDisplayed()));
    }
}
