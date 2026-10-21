package io.github.lucaslimacelino_dev.calju.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import android.os.SystemClock;
import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.NoMatchingViewException;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.MainActivity;
import io.github.lucaslimacelino_dev.calju.R;

@RunWith(AndroidJUnit4.class)
public class DiaryInputTest {

    private static final String ARROZ = "Arroz, tipo 1, cozido";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Começa cada teste com diário e metas vazios (o catálogo continua). A transação do Room faz
     * o banco avisar a tela da mudança; um execSQL solto não avisaria.
     */
    @Before
    public void clearData() {
        CaLjuApp app = ApplicationProvider.getApplicationContext();
        app.container().database.runInTransaction(() -> {
            app.container().database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM log_entries");
            app.container().database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM nutrition_goals");
        });
    }

    /** Busca e banco rodam fora da thread da tela; espera até 10 s pela condição. */
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

    private static Matcher<View> inEntries(Matcher<View> matcher) {
        return allOf(matcher, isDescendantOfA(withId(R.id.entries)));
    }

    private static void addArroz(String grams) {
        onView(withId(R.id.food_input)).perform(replaceText("arroz tipo 1 cozido"));
        waitUntilDisplayed(allOf(withText(ARROZ), isDescendantOfA(withId(R.id.suggestions))));
        onView(allOf(withText(ARROZ), isDescendantOfA(withId(R.id.suggestions)))).perform(click());
        waitUntilDisplayed(withId(R.id.grams));
        onView(withId(R.id.occasion_dinner)).perform(click());
        onView(withId(R.id.grams)).perform(replaceText(grams));
        onView(withId(R.id.add_button)).perform(click());
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
    public void adicionarGravaNoDiarioEAtualizaOPainel() {
        addArroz("150");

        waitUntilDisplayed(withText("Adicionado: jantar"));
        waitUntilDisplayed(inEntries(withText(ARROZ)));
        onView(inEntries(withText("150 g"))).check(matches(isDisplayed()));
        waitUntilDisplayed(allOf(withId(R.id.kcal_value), withText("192")));
        onView(withId(R.id.food_input)).check(matches(withText("")));
    }

    @Test
    public void editarQuantidadeAtualizaORegistro() {
        addArroz("150");
        waitUntilDisplayed(inEntries(withText(ARROZ)));

        onView(inEntries(withText(ARROZ))).perform(click());
        waitUntilDisplayed(withText(R.string.quantity_save));
        onView(withId(R.id.grams)).check(matches(withText("150")));
        onView(withId(R.id.grams)).perform(replaceText("100"));
        onView(withId(R.id.add_button)).perform(click());

        waitUntilDisplayed(inEntries(withText("100 g")));
        waitUntilDisplayed(allOf(withId(R.id.kcal_value), withText("128")));
    }

    @Test
    public void removerEDesfazer() {
        addArroz("150");
        waitUntilDisplayed(inEntries(withText(ARROZ)));

        onView(inEntries(withText(ARROZ))).perform(click());
        waitUntilDisplayed(withId(R.id.remove_button));
        onView(withId(R.id.remove_button)).perform(click());

        waitUntilDisplayed(withId(R.id.empty_state));
        onView(withText(R.string.undo)).perform(click());
        waitUntilDisplayed(inEntries(withText(ARROZ)));
    }

    @Test
    public void quantidadeVaziaMostraErroEMantemATelaAberta() {
        onView(withId(R.id.food_input)).perform(replaceText("arroz tipo 1 cozido"));
        waitUntilDisplayed(allOf(withText(ARROZ), isDescendantOfA(withId(R.id.suggestions))));
        onView(allOf(withText(ARROZ), isDescendantOfA(withId(R.id.suggestions)))).perform(click());

        waitUntilDisplayed(withId(R.id.add_button));
        onView(withId(R.id.add_button)).perform(click());

        onView(withText(R.string.quantity_invalid)).check(matches(isDisplayed()));
        onView(withId(R.id.add_button)).check(matches(isDisplayed()));
    }
}
