package io.github.lucaslimacelino_dev.calju.ui;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
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
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/** Quantidades e meta simulam o uso; os kcal esperados vêm do catálogo (arroz: 128,2585 kcal/100 g). */
@RunWith(AndroidJUnit4.class)
public class HistoryFlowTest {

    @Rule
    public ActivityScenarioRule<MainActivity> activityRule = new ActivityScenarioRule<>(MainActivity.class);

    private AppContainer container;
    private LocalDate today;

    @Before
    public void seed() throws Exception {
        CaLjuApp app = ApplicationProvider.getApplicationContext();
        container = app.container();
        today = LocalDate.now(container.clock);
        container.catalogImporter.importIfNeeded();
        container.database.runInTransaction(() -> {
            container.database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM log_entries");
            container.database.getOpenHelper().getWritableDatabase().execSQL("DELETE FROM nutrition_goals");
        });
        String arroz = container.catalogRepository.search("arroz tipo 1 cozido", 1).get(0).id;
        container.goalRepository.save(new Goal(today.minusDays(30), new BigDecimal("2000"), null, null, null));
        container.diaryRepository.addFood(today.minusDays(1), Occasion.LUNCH, arroz, new BigDecimal("100"));
        container.diaryRepository.addFood(today.minusDays(3), Occasion.LUNCH, arroz, new BigDecimal("300"));
    }

    @Test
    public void mostraDiasComTotaisMetaEMedia() {
        onView(withId(R.id.historyFragment)).perform(click());

        Waits.untilPresent(allOf(withText("Ontem"), isDescendantOfA(withId(R.id.days))));
        Waits.untilPresent(withText("128 de 2.000 kcal"));
        Waits.untilPresent(withText("385 de 2.000 kcal"));
        // (128,2585 + 384,7755) ÷ 2 = 256,517 → exibido como 257.
        Waits.untilPresent(allOf(withId(R.id.average_value), withText("257 kcal por dia")));
    }

    @Test
    public void tocarNumDiaAbreODiarioNaqueleDia() {
        onView(withId(R.id.historyFragment)).perform(click());
        Waits.untilPresent(allOf(withText("Ontem"), isDescendantOfA(withId(R.id.days))));

        onView(allOf(withText("Ontem"), isDescendantOfA(withId(R.id.days)))).perform(scrollTo(), click());

        Waits.untilDisplayed(allOf(withId(R.id.day_title), withText("Ontem")));
        Waits.untilPresent(allOf(withId(R.id.kcal_value), withText("1.872")));
    }
}
