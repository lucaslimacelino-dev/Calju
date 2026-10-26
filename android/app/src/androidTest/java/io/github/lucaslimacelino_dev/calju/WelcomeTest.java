package io.github.lucaslimacelino_dev.calju;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/** A história de boas-vindas aparece só na primeira abertura e leva ao questionário da meta. */
@RunWith(AndroidJUnit4.class)
public class WelcomeTest {

    private Context context;

    @Before
    public void forgetWelcome() {
        context = ApplicationProvider.getApplicationContext();
        context.getSharedPreferences("calju", Context.MODE_PRIVATE).edit().clear().commit();
    }

    @Test
    public void primeiraAberturaMostraAsDuasPaginasEOQuestionario() {
        try (ActivityScenario<WelcomeActivity> ignored = ActivityScenario.launch(WelcomeActivity.class)) {
            onView(withText(R.string.welcome_hello_title)).check(matches(isDisplayed()));
            onView(withId(R.id.next)).perform(click());

            onView(withText(R.string.welcome_how_title)).check(matches(isDisplayed()));
            assertFalse(WelcomeActivity.wasSeen(context));
            onView(withId(R.id.next)).perform(click());

            onView(withText(R.string.quiz_q_objective)).check(matches(isDisplayed()));
            assertTrue(WelcomeActivity.wasSeen(context));
            onView(withId(R.id.skip)).perform(click());

            onView(withId(R.id.food_input)).check(matches(isDisplayed()));
        }
    }

    @Test
    public void depoisDeVistaAbreDiretoNoDiario() {
        WelcomeActivity.markSeen(context);

        try (ActivityScenario<WelcomeActivity> ignored = ActivityScenario.launch(WelcomeActivity.class)) {
            onView(withId(R.id.food_input)).check(matches(isDisplayed()));
        }
    }
}
