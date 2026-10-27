package io.github.lucaslimacelino_dev.calju;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Activity;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Objective;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Sex;
import io.github.lucaslimacelino_dev.calju.domain.UserProfile;

/** Boas-vindas na primeira abertura, depois o questionário obrigatório; sem respostas, ele volta. */
@RunWith(AndroidJUnit4.class)
public class WelcomeTest {

    private Context context;
    private AppContainer container;

    @Before
    public void forgetWelcome() {
        context = ApplicationProvider.getApplicationContext();
        container = ((CaLjuApp) context).container();
        context.getSharedPreferences("calju", Context.MODE_PRIVATE).edit().clear().commit();
        container.profileStore.clear();
    }

    @Test
    public void primeiraAberturaMostraAsDuasPaginasEOQuestionarioSemPular() {
        try (ActivityScenario<WelcomeActivity> ignored = ActivityScenario.launch(WelcomeActivity.class)) {
            onView(withText(R.string.welcome_hello_title)).check(matches(isDisplayed()));
            onView(withId(R.id.next)).perform(click());

            onView(withText(R.string.welcome_how_title)).check(matches(isDisplayed()));
            assertFalse(WelcomeActivity.wasSeen(context));
            onView(withId(R.id.next)).perform(click());

            onView(withText(R.string.quiz_q_objective)).check(matches(isDisplayed()));
            assertTrue(WelcomeActivity.wasSeen(context));
            onView(withText("Pular")).check(doesNotExist());
        }
    }

    @Test
    public void comRespostasGuardadasAbreDiretoNoDiario() {
        WelcomeActivity.markSeen(context);
        container.profileStore.save(UserProfile.fromAge(Sex.FEMALE, 30, LocalDate.now(container.clock),
                new BigDecimal("165"), new BigDecimal("60"), Activity.LIGHT, Objective.KEEP, null, false));

        try (ActivityScenario<WelcomeActivity> ignored = ActivityScenario.launch(WelcomeActivity.class)) {
            onView(withId(R.id.food_input)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void semRespostasGuardadasOQuestionarioVolta() {
        WelcomeActivity.markSeen(context);

        try (ActivityScenario<WelcomeActivity> ignored = ActivityScenario.launch(WelcomeActivity.class)) {
            onView(withText(R.string.quiz_q_objective)).check(matches(isDisplayed()));
        }
    }
}
