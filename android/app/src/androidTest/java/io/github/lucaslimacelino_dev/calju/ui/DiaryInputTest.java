package io.github.lucaslimacelino_dev.calju.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;

import android.os.SystemClock;
import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.MainActivity;
import io.github.lucaslimacelino_dev.calju.R;

@RunWith(AndroidJUnit4.class)
public class DiaryInputTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    /** A busca e a importação do catálogo rodam fora da thread da tela; espera até 10 s. */
    private static void waitUntilDisplayed(Matcher<View> matcher) {
        long deadline = SystemClock.uptimeMillis() + 10_000;
        while (true) {
            try {
                onView(matcher).check(matches(isDisplayed()));
                return;
            } catch (NoMatchingViewException | AssertionError e) {
                if (SystemClock.uptimeMillis() > deadline) {
                    throw e;
                }
                SystemClock.sleep(100);
            }
        }
    }

    @Test
    public void unidadesUsamAPorcaoPadraoEAbremATelaDeQuantidadePreenchida() {
        onView(withId(R.id.food_input)).perform(replaceText("2 bananas prata"));

        waitUntilDisplayed(withText(containsString("2 × 1 unidade · 150 g")));
        onView(withText("Banana, prata, crua")).perform(click());

        waitUntilDisplayed(withId(R.id.grams));
        onView(withId(R.id.grams)).check(matches(withText("150")));
        onView(withId(R.id.portion_button)).check(matches(withText(containsString("1 unidade (75 g)"))));
    }

    @Test
    public void semQuantidadeOAppPerguntaEConfirmaComOValorDigitado() {
        onView(withId(R.id.food_input)).perform(replaceText("arroz tipo 1 cozido"));

        waitUntilDisplayed(withText(containsString("kcal em 100 g")));
        onView(withText("Arroz, tipo 1, cozido")).perform(click());

        waitUntilDisplayed(withId(R.id.grams));
        onView(withId(R.id.grams)).check(matches(withText("")));
        onView(withId(R.id.grams)).perform(replaceText("150"));
        onView(withId(R.id.add_button)).perform(click());

        waitUntilDisplayed(withText(containsString("Arroz, tipo 1, cozido · 150 g")));
        onView(withId(R.id.food_input)).check(matches(withText("")));
    }

    @Test
    public void quantidadeVaziaMostraErroEMantemATelaAberta() {
        onView(withId(R.id.food_input)).perform(replaceText("arroz tipo 1 cozido"));
        waitUntilDisplayed(withText("Arroz, tipo 1, cozido"));
        onView(withText("Arroz, tipo 1, cozido")).perform(click());

        waitUntilDisplayed(withId(R.id.add_button));
        onView(withId(R.id.add_button)).perform(click());

        onView(withText(R.string.quantity_invalid)).check(matches(isDisplayed()));
        onView(withId(R.id.add_button)).check(matches(isDisplayed()));
    }
}
