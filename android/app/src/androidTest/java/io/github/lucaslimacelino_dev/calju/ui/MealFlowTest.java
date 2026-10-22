package io.github.lucaslimacelino_dev.calju.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.hasSibling;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;

import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.MainActivity;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.data.local.MealItemEntity;
import io.github.lucaslimacelino_dev.calju.data.repository.ItemDraft;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

@RunWith(AndroidJUnit4.class)
public class MealFlowTest {

    private static final String ARROZ = "Arroz, tipo 1, cozido";
    private static final String FEIJAO = "Feijão, carioca, cozido";

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private AppContainer container;

    @Before
    public void clearData() throws Exception {
        CaLjuApp app = ApplicationProvider.getApplicationContext();
        container = app.container();
        container.catalogImporter.importIfNeeded();
        container.database.runInTransaction(() -> {
            container.database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM log_entries");
            container.database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM nutrition_goals");
            container.database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM meals");
        });
    }

    /** Cria a refeição direto no repositório (fora da thread da tela), para testar o uso dela. */
    private String createMarmita() {
        String arroz = container.catalogRepository.search("arroz tipo 1 cozido", 1).get(0).id;
        String feijao = container.catalogRepository.search("feijao carioca cozido", 1).get(0).id;
        return container.mealRepository.create("Marmita", Arrays.asList(
                ItemDraft.of(arroz, new BigDecimal("150")), ItemDraft.of(feijao, new BigDecimal("100"))));
    }

    private static Matcher<View> inEntries(Matcher<View> matcher) {
        return allOf(matcher, isDescendantOfA(withId(R.id.entries)));
    }

    private static Matcher<View> inMealsList(Matcher<View> matcher) {
        return allOf(matcher, isDescendantOfA(withId(R.id.meals_list)));
    }

    private static void openMeals() {
        Waits.untilDayLoaded();
        Waits.scrollAndTap(withId(R.id.meals_header));
    }

    /** Toca num elemento depois que o aviso sumir (ele cobre a parte de baixo da tela). */
    private static void tapWhenFree(Matcher<View> matcher) {
        Waits.untilPresent(matcher);
        Waits.untilSnackbarGone();
        Waits.scrollAndTap(matcher);
    }

    private static void includeInEditor(String text, String foodName) {
        onView(withId(R.id.search)).perform(scrollTo(), replaceText(text));
        Matcher<View> suggestion = allOf(withText(foodName), isDescendantOfA(withId(R.id.suggestions)));
        Waits.untilDisplayed(suggestion);
        onView(suggestion).perform(click());
    }

    private static Matcher<View> gramsOf(String foodName) {
        return allOf(withId(R.id.grams), hasSibling(withText(foodName)));
    }

    @Test
    public void criarRefeicaoEAdicionarAoDiaComoEsta() {
        openMeals();
        onView(withId(R.id.new_meal)).perform(scrollTo(), click());

        Waits.untilDisplayed(withId(R.id.name));
        onView(withId(R.id.name)).perform(replaceText("Marmita"));
        includeInEditor("arroz tipo 1 cozido 150", ARROZ);
        includeInEditor("feijao carioca cozido 100", FEIJAO);
        Waits.untilPresent(gramsOf(FEIJAO));
        onView(withId(R.id.primary_button)).perform(scrollTo(), click());

        Waits.untilDisplayed(withText("Refeição salva: Marmita"));
        tapWhenFree(inMealsList(withId(R.id.add)));

        Waits.untilPresent(inEntries(withText("Marmita")));
        Waits.untilPresent(inEntries(withText("2 itens · 250 g")));
    }

    @Test
    public void adicionarHojeComAjusteNaoAlteraOMolde() {
        String mealId = createMarmita();
        openMeals();
        tapWhenFree(inMealsList(withId(R.id.edit)));

        Waits.untilPresent(gramsOf(ARROZ));
        onView(gramsOf(ARROZ)).perform(scrollTo(), replaceText("200"));
        onView(withId(R.id.occasion_dinner)).perform(scrollTo(), click());
        onView(withId(R.id.primary_button)).perform(scrollTo(), click());

        Waits.untilPresent(inEntries(withText("2 itens · 300 g")));
        for (MealItemEntity item : container.mealRepository.byId(mealId).items) {
            if (item.position == 0) {
                assertEquals(0, new BigDecimal("150").compareTo(item.quantityG));
            }
        }
    }

    @Test
    public void editarRegistroDaRefeicaoNoDiario() {
        createMarmita();
        openMeals();
        tapWhenFree(inMealsList(withId(R.id.add)));
        Waits.untilPresent(inEntries(withText("2 itens · 250 g")));

        tapWhenFree(inEntries(withText("Marmita")));
        Waits.untilPresent(gramsOf(ARROZ));
        onView(gramsOf(ARROZ)).perform(scrollTo(), replaceText("50"));
        onView(withId(R.id.primary_button)).perform(scrollTo(), click());

        Waits.untilPresent(inEntries(withText("2 itens · 150 g")));
    }

    @Test
    public void salvarRegistroComoRefeicaoCriaUmMoldeIgual() {
        String arroz = container.catalogRepository.search("arroz tipo 1 cozido", 1).get(0).id;
        String feijao = container.catalogRepository.search("feijao carioca cozido", 1).get(0).id;
        container.diaryRepository.addItems(LocalDate.now(container.clock),
                Occasion.LUNCH, "Almoço de hoje", null,
                Arrays.asList(ItemDraft.of(arroz, new BigDecimal("120")), ItemDraft.of(feijao, new BigDecimal("80"))));

        tapWhenFree(inEntries(withText("Almoço de hoje")));
        Waits.untilDisplayed(withText(R.string.entry_save_as_meal));
        onView(withText(R.string.entry_save_as_meal)).perform(scrollTo(), click());

        Waits.untilDisplayed(withText("Refeição salva: Almoço de hoje"));
        assertEquals(1, container.mealRepository.all().size());
        assertEquals(2, container.mealRepository.all().get(0).items.size());
    }
}
