package io.github.lucaslimacelino_dev.calju.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.Visibility.VISIBLE;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;

import android.os.SystemClock;
import android.view.View;

import androidx.test.espresso.NoMatchingViewException;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.ViewAssertion;

import org.hamcrest.Matcher;

import io.github.lucaslimacelino_dev.calju.R;

/**
 * Esperas dos testes de tela. Busca e banco rodam fora da thread da tela, e o Espresso não sabe
 * quando terminam; por isso as verificações são repetidas por até 10 s.
 */
final class Waits {

    private static final long TIMEOUT_MS = 10_000;

    private Waits() {
    }

    /** Espera a view aparecer na área visível da tela. */
    static void untilDisplayed(Matcher<View> matcher) {
        until(matcher, matches(isDisplayed()));
    }

    /** Espera a view existir e estar visível, mesmo que fora da área rolada da tela. */
    static void untilPresent(Matcher<View> matcher) {
        until(matcher, matches(withEffectiveVisibility(VISIBLE)));
    }

    /**
     * Espera o dia carregar (o painel ganha número). Antes disso o conteúdo do Diário ainda muda de
     * altura, e uma rolagem feita cedo demais deixa o alvo fora da tela.
     */
    static void untilDayLoaded() {
        untilPresent(allOf(withId(R.id.kcal_value), withText(not(""))));
    }

    /**
     * Rola até a view e toca nela. A rolagem deixa o alvo encostado na borda de baixo; se a tela
     * mudar de tamanho logo depois (teclado, dados chegando), o toque falha e é tentado de novo.
     */
    static void scrollAndTap(Matcher<View> matcher) {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (true) {
            try {
                onView(matcher).perform(scrollTo(), click());
                return;
            } catch (PerformException | NoMatchingViewException e) {
                if (SystemClock.uptimeMillis() > deadline) {
                    throw e;
                }
                SystemClock.sleep(100);
            }
        }
    }

    /** O aviso (Snackbar) cobre a parte de baixo da tela por alguns segundos; espera ele sumir. */
    static void untilSnackbarGone() {
        until(withId(com.google.android.material.R.id.snackbar_text), doesNotExist());
    }

    private static void until(Matcher<View> matcher, ViewAssertion assertion) {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (true) {
            try {
                onView(matcher).check(assertion);
                return;
            } catch (NoMatchingViewException | AssertionError e) {
                if (SystemClock.uptimeMillis() > deadline) {
                    throw e;
                }
                SystemClock.sleep(100);
            }
        }
    }
}
