package io.github.lucaslimacelino_dev.calju.ui.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.ItemWithNutrients;
import io.github.lucaslimacelino_dev.calju.data.local.LogEntryEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogItemEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogItemNutrientEntity;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

public class HistoryBuilderTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);
    private static int counter;

    // Valores fictícios: só a aritmética e o agrupamento importam.
    private static EntryWithItems entry(LocalDate date, String grams, String kcalPer100g) {
        String id = "e" + (counter++);
        EntryWithItems entry = new EntryWithItems();
        entry.entry = new LogEntryEntity(id, date, Occasion.LUNCH, null, null, 0, 0, null, 1, "PENDING");
        ItemWithNutrients item = new ItemWithNutrients();
        item.item = new LogItemEntity("i" + id, id, "food", "Alimento", null, null, new BigDecimal(grams), null, 0);
        item.nutrients = new ArrayList<>();
        if (kcalPer100g != null) {
            item.nutrients.add(new LogItemNutrientEntity(item.item.id, Nutrients.ENERGY,
                    new BigDecimal(kcalPer100g), "VALUE"));
        }
        entry.items = Collections.singletonList(item);
        return entry;
    }

    @Test
    public void agrupaPorDiaDoMaisRecenteParaOMaisAntigo() {
        HistoryState state = HistoryBuilder.build(TODAY, Arrays.asList(
                entry(TODAY.minusDays(2), "100", "100"),
                entry(TODAY, "100", "50"),
                entry(TODAY.minusDays(2), "50", "100")), Collections.emptyList());

        assertEquals(2, state.days().size());
        assertEquals(TODAY, state.days().get(0).date());
        assertEquals(0, new BigDecimal("150").compareTo(state.days().get(1).kcal().amount()));
    }

    @Test
    public void mediaUsaSoDiasComRegistroNosSeteDiasAntesDeHoje() {
        HistoryState state = HistoryBuilder.build(TODAY, Arrays.asList(
                entry(TODAY, "100", "900"),
                entry(TODAY.minusDays(1), "100", "100"),
                entry(TODAY.minusDays(3), "100", "200"),
                entry(TODAY.minusDays(8), "100", "1000")), Collections.emptyList());

        assertEquals(2, state.weekDaysWithRecords());
        assertEquals(0, new BigDecimal("150").compareTo(state.weekAverage()));
        assertTrue(state.weekAverageComplete());
    }

    @Test
    public void semDiasNaSemanaAMediaFicaNula() {
        HistoryState state = HistoryBuilder.build(TODAY,
                Collections.singletonList(entry(TODAY, "100", "100")), Collections.emptyList());

        assertNull(state.weekAverage());
        assertEquals(0, state.weekDaysWithRecords());
    }

    @Test
    public void energiaDesconhecidaDeixaAMediaIncompleta() {
        HistoryState state = HistoryBuilder.build(TODAY,
                Collections.singletonList(entry(TODAY.minusDays(1), "100", null)), Collections.emptyList());

        assertFalse(state.weekAverageComplete());
    }

    @Test
    public void cadaDiaUsaAMetaQueValiaNaquelaData() {
        List<Goal> goals = Arrays.asList(
                new Goal(TODAY.minusDays(10), new BigDecimal("1000"), null, null, null),
                new Goal(TODAY, new BigDecimal("2000"), null, null, null));

        HistoryState state = HistoryBuilder.build(TODAY, Arrays.asList(
                entry(TODAY, "100", "100"), entry(TODAY.minusDays(1), "100", "100")), goals);

        assertEquals(0, new BigDecimal("2000").compareTo(state.days().get(0).goal().kcal()));
        assertEquals(0, new BigDecimal("1000").compareTo(state.days().get(1).goal().kcal()));
    }
}
