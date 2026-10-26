package io.github.lucaslimacelino_dev.calju.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.MainActivity;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/** 400 g de arroz tipo 1 cozido (TACO: fibra 1,561 g, zinco 0,4913 mg por 100 g; sem vitamina C). */
@RunWith(AndroidJUnit4.class)
public class MicronutrientsTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void seed() throws Exception {
        CaLjuApp app = ApplicationProvider.getApplicationContext();
        AppContainer container = app.container();
        container.catalogImporter.importIfNeeded();
        container.database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM log_entries");
        String arroz = container.catalogRepository.search("arroz tipo 1 cozido", 1).get(0).id;
        container.diaryRepository.addFood(LocalDate.now(container.clock), Occasion.LUNCH, arroz,
                new BigDecimal("400"));
    }

    @Test
    public void mostraGruposComPorcentagemEExplicacao() {
        Waits.untilDayLoaded();
        Waits.scrollAndTap(withId(R.id.micronutrients_button));

        Waits.untilDisplayed(withText(R.string.micronutrients_group_minerals));
        // Fibras: 6,244 g de 25 g = 25%. Zinco: 1,965 mg de 11 mg = 18%.
        onView(allOf(withId(R.id.percent), withText("25%"))).check(matches(isDisplayed()));
        onView(withContentDescription("Zinco, 2,0 mg. 18 por cento da referência do dia"))
                .perform(scrollTo()).check(matches(isDisplayed()));
        onView(withText("Umidade")).check(doesNotExist());
        onView(withText("Cinzas")).check(doesNotExist());

        onView(withText("Fibras")).perform(click());
        onView(withText(R.string.nutrient_info_FIBTG)).check(matches(isDisplayed()));

        onView(withContentDescription("Vitamina C, —")).perform(scrollTo()).check(matches(isDisplayed()));
    }
}
