package io.github.lucaslimacelino_dev.calju.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.MainActivity;
import io.github.lucaslimacelino_dev.calju.R;

/** Os números digitados aqui simulam o que um usuário escolheria; não são recomendações. */
@RunWith(AndroidJUnit4.class)
public class GoalFlowTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void clearData() {
        CaLjuApp app = ApplicationProvider.getApplicationContext();
        app.container().database.runInTransaction(() -> {
            app.container().database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM log_entries");
            app.container().database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM nutrition_goals");
        });
    }

    @Test
    public void semMetaODiarioOfereceDefinirMeta() {
        Waits.untilDisplayed(withId(R.id.define_goal));
        onView(withText(R.string.kcal_consumed)).check(matches(isDisplayed()));
    }

    @Test
    public void definirMetaNoPerfilMostraKcalRestantesNoDiario() {
        Waits.untilDisplayed(withId(R.id.define_goal));
        onView(withId(R.id.define_goal)).perform(click());

        Waits.untilDisplayed(withId(R.id.kcal));
        onView(withId(R.id.kcal)).perform(replaceText("2000"));
        onView(withId(R.id.protein)).perform(replaceText("100"));
        onView(withId(R.id.save_goals)).perform(scrollTo(), click());
        Waits.untilDisplayed(withText(R.string.goals_saved));

        onView(withId(R.id.diaryFragment)).perform(click());
        Waits.untilDisplayed(withText("kcal restantes · meta 2.000"));
        onView(allOf(withId(R.id.kcal_value), withText("2.000"))).check(matches(isDisplayed()));
        onView(withText("de 100 g")).check(matches(isDisplayed()));
    }

    @Test
    public void metaSemCaloriasMostraErro() {
        onView(withId(R.id.profileFragment)).perform(click());
        Waits.untilDisplayed(withId(R.id.save_goals));

        onView(withId(R.id.save_goals)).perform(scrollTo(), click());

        onView(withText(R.string.goals_kcal_required)).check(matches(isDisplayed()));
    }
}
