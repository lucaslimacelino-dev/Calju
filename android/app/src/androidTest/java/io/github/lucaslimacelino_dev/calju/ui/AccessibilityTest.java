package io.github.lucaslimacelino_dev.calju.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.MainActivity;
import io.github.lucaslimacelino_dev.calju.R;

/**
 * Verificações automáticas de acessibilidade (Accessibility Test Framework do Google) em cada aba:
 * área de toque, contraste, rótulos para o TalkBack, itens repetidos. Qualquer erro reprova.
 */
@RunWith(AndroidJUnit4.class)
public class AccessibilityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private final AccessibilityValidator validator = new AccessibilityValidator().setRunChecksFromRootView(true);

    private void checkScreen() {
        activityRule.getScenario().onActivity(activity -> validator.check(activity.getWindow().getDecorView()));
    }

    @Test
    public void diario() {
        onView(withId(R.id.food_input)).check(matches(isDisplayed()));
        Waits.untilDayLoaded();
        checkScreen();
    }

    @Test
    public void historico() {
        onView(withId(R.id.historyFragment)).perform(click());
        checkScreen();
    }

    @Test
    public void perfil() {
        onView(withId(R.id.profileFragment)).perform(click());
        checkScreen();
    }
}
