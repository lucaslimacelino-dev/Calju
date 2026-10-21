package io.github.lucaslimacelino_dev.calju;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void abreNoDiario() {
        onView(withId(R.id.food_input)).check(matches(isDisplayed()));
        onView(withText(R.string.day_today)).check(matches(isDisplayed()));
    }

    @Test
    public void navegaEntreAsAbas() {
        onView(withId(R.id.profileFragment)).perform(click());
        onView(withText(R.string.goals_title)).check(matches(isDisplayed()));

        onView(withId(R.id.historyFragment)).perform(click());
        onView(withText(R.string.history_empty)).check(matches(isDisplayed()));

        onView(withId(R.id.diaryFragment)).perform(click());
        onView(withId(R.id.food_input)).check(matches(isDisplayed()));
    }
}
