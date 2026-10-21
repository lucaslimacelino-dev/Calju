package io.github.lucaslimacelino_dev.calju.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.repository.GoalRepository;
import io.github.lucaslimacelino_dev.calju.domain.Goal;

@RunWith(AndroidJUnit4.class)
public class GoalRepositoryTest {

    private CaljuDatabase database;
    private GoalRepository goals;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, CaljuDatabase.class).build();
        goals = new GoalRepository(database, Clock.systemUTC());
    }

    @After
    public void tearDown() {
        database.close();
    }

    // Valores fictícios: o teste verifica gravação e vigência, não recomendações nutricionais.
    private static Goal goal(String from, String kcal, String protein) {
        return new Goal(LocalDate.parse(from), new BigDecimal(kcal),
                protein == null ? null : new BigDecimal(protein), null, null);
    }

    @Test
    public void salvaEMantemAsMetasAnteriores() {
        goals.save(goal("2026-10-10", "1000", "50"));
        goals.save(goal("2026-10-20", "1200", null));

        List<Goal> all = goals.all();
        assertEquals(2, all.size());
        assertEquals(0, new BigDecimal("1000").compareTo(Goal.forDate(all, LocalDate.parse("2026-10-15")).get().kcal()));
        assertEquals(0, new BigDecimal("1200").compareTo(Goal.forDate(all, LocalDate.parse("2026-10-21")).get().kcal()));
        assertNull(Goal.forDate(all, LocalDate.parse("2026-10-21")).get().proteinG());
    }

    @Test
    public void salvarDeNovoNaMesmaDataCorrigeEmVezDeDuplicar() {
        goals.save(goal("2026-10-20", "1000", null));
        goals.save(goal("2026-10-20", "1100", "60"));

        List<Goal> all = goals.all();
        assertEquals(1, all.size());
        assertEquals(0, new BigDecimal("1100").compareTo(all.get(0).kcal()));
        assertEquals(0, new BigDecimal("60").compareTo(all.get(0).proteinG()));
        assertEquals(2, database.goalDao().all().get(0).version);
    }
}
