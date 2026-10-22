package io.github.lucaslimacelino_dev.calju.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.View;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.MainActivity;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.data.repository.CustomFoodData;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;

@RunWith(AndroidJUnit4.class)
public class CustomFoodFlowTest {

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
            container.database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM meals");
            container.database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM nutrition_goals");
            container.database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM foods WHERE is_custom = 1");
        });
    }

    private static Matcher<View> field(int layoutId) {
        return allOf(isDescendantOfA(withId(layoutId)), withId(fieldOf(layoutId)));
    }

    private static int fieldOf(int layoutId) {
        if (layoutId == R.id.name_layout) {
            return R.id.name;
        }
        if (layoutId == R.id.portion_label_layout) {
            return R.id.portion_label;
        }
        if (layoutId == R.id.portion_grams_layout) {
            return R.id.portion_grams;
        }
        if (layoutId == R.id.kcal_layout) {
            return R.id.kcal;
        }
        if (layoutId == R.id.protein_layout) {
            return R.id.protein;
        }
        if (layoutId == R.id.carbohydrate_layout) {
            return R.id.carbohydrate;
        }
        return R.id.fat;
    }

    private static void type(int layoutId, String text) {
        onView(field(layoutId)).perform(scrollTo(), replaceText(text));
    }

    private static void fillMacros(String kcal, String protein, String carbohydrate, String fat) {
        type(R.id.kcal_layout, kcal);
        type(R.id.protein_layout, protein);
        type(R.id.carbohydrate_layout, carbohydrate);
        type(R.id.fat_layout, fat);
    }

    // Valores fictícios de rótulo: só o comportamento importa.
    @Test
    public void criarPeloDiarioComValoresPorPorcaoERegistrar() {
        onView(withId(R.id.food_input)).perform(replaceText("granola da casa 40"));
        Waits.untilDisplayed(withId(R.id.create_food));
        onView(withId(R.id.create_food)).perform(click());

        Waits.untilDisplayed(field(R.id.name_layout));
        onView(field(R.id.name_layout)).check(matches(withText("Granola da casa")));
        onView(withId(R.id.basis_portion)).perform(scrollTo(), click());
        type(R.id.portion_label_layout, "1 xícara");
        type(R.id.portion_grams_layout, "40");
        fillMacros("160", "4", "24", "5");
        onView(withId(R.id.save_button)).perform(scrollTo(), click());

        Waits.untilDisplayed(withId(R.id.add_button));
        onView(withId(R.id.grams)).check(matches(withText("40")));
        onView(withId(R.id.add_button)).perform(click());

        Waits.untilPresent(allOf(withText("Granola da casa"), isDescendantOfA(withId(R.id.entries))));
        Waits.untilPresent(allOf(withId(R.id.kcal_value), withText("160")));
        String id = container.catalogRepository.search("granola da casa", 1).get(0).id;
        assertEquals(0, new BigDecimal("400")
                .compareTo(container.catalogRepository.customFood(id).per100g().get(Nutrients.ENERGY)));
    }

    @Test
    public void macrosMaisPesadosQueAPorcaoMostramErroENaoSalvam() {
        onView(withId(R.id.food_input)).perform(replaceText("biscoito teste"));
        Waits.untilDisplayed(withId(R.id.create_food));
        onView(withId(R.id.create_food)).perform(click());

        Waits.untilDisplayed(field(R.id.name_layout));
        onView(withId(R.id.basis_portion)).perform(scrollTo(), click());
        type(R.id.portion_grams_layout, "30");
        fillMacros("150", "20", "20", "5");
        onView(withId(R.id.save_button)).perform(scrollTo(), click());

        onView(withId(R.id.form_error)).perform(scrollTo()).check(matches(isDisplayed()));
        assertTrue(container.catalogRepository.search("biscoito teste", 5).isEmpty());
    }

    @Test
    public void editarEExcluirComDesfazerNoPerfil() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put(Nutrients.ENERGY, new BigDecimal("380"));
        values.put(Nutrients.PROTEIN, new BigDecimal("8"));
        values.put(Nutrients.CARBOHYDRATE, new BigDecimal("70"));
        values.put(Nutrients.FAT, new BigDecimal("6"));
        String id = container.catalogRepository.createCustomFood(
                new CustomFoodData("Bolacha caseira", null, values, null, null));

        onView(withId(R.id.profileFragment)).perform(click());
        Matcher<View> editButton = allOf(withId(R.id.edit), withContentDescription("Editar Bolacha caseira"));
        Waits.scrollAndTap(editButton);

        Waits.untilDisplayed(allOf(field(R.id.kcal_layout), withText("380")));
        type(R.id.kcal_layout, "390");
        onView(withId(R.id.save_button)).perform(scrollTo(), click());
        Waits.untilDisplayed(withText("Alimento salvo: Bolacha caseira"));
        assertEquals(0, new BigDecimal("390")
                .compareTo(container.catalogRepository.customFood(id).per100g().get(Nutrients.ENERGY)));

        Waits.untilSnackbarGone();
        Waits.scrollAndTap(editButton);
        Waits.untilDisplayed(withId(R.id.delete_button));
        onView(withId(R.id.delete_button)).perform(scrollTo(), click());
        Waits.untilDisplayed(withText("Alimento excluído: Bolacha caseira"));
        onView(withText(R.string.undo)).perform(click());

        Waits.untilPresent(editButton);
        assertEquals(id, container.catalogRepository.search("bolacha caseira", 1).get(0).id);
    }
}
