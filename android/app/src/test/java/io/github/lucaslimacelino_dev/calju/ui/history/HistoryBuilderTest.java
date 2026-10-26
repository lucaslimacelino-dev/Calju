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

    /** Quarta-feira; a semana começa na segunda, 19/10. */
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);
    private static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);
    private static final LocalDate EARLIEST = MONDAY.minusWeeks(11);
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
            item.nutrients.add(new LogItemNutrientEntity(item.item.id, Nutrients.PROTEIN, BigDecimal.TEN, "VALUE"));
            item.nutrients.add(new LogItemNutrientEntity(item.item.id, Nutrients.CARBOHYDRATE, BigDecimal.TEN,
                    "VALUE"));
            item.nutrients.add(new LogItemNutrientEntity(item.item.id, Nutrients.FAT, BigDecimal.TEN, "VALUE"));
        }
        entry.items = Collections.singletonList(item);
        return entry;
    }

    private static HistoryState week(LocalDate weekStart, List<EntryWithItems> entries, List<Goal> goals) {
        return HistoryBuilder.build(TODAY, weekStart, EARLIEST, entries, goals);
    }

    @Test
    public void semanaComecaNaSegunda() {
        assertEquals(MONDAY, HistoryBuilder.weekStart(TODAY));
        assertEquals(MONDAY, HistoryBuilder.weekStart(MONDAY));
        assertEquals(MONDAY, HistoryBuilder.weekStart(LocalDate.of(2026, 10, 25)));
    }

    @Test
    public void barrasCobremOsSeteDiasEMarcamHojeEFuturo() {
        HistoryState state = week(MONDAY, Arrays.asList(
                entry(MONDAY, "100", "100"),
                entry(MONDAY, "50", "100"),
                entry(TODAY, "100", "50")), Collections.emptyList());

        assertEquals(7, state.bars().size());
        assertEquals(0, new BigDecimal("150").compareTo(state.bars().get(0).kcal()));
        assertNull(state.bars().get(1).kcal());
        assertTrue(state.bars().get(2).today());
        assertTrue(state.bars().get(3).future());
        assertFalse(state.bars().get(1).future());
    }

    @Test
    public void listaTrazSoOsDiasDaSemanaDoMaisRecente() {
        HistoryState state = week(MONDAY, Arrays.asList(
                entry(MONDAY, "100", "100"),
                entry(TODAY, "100", "100"),
                entry(MONDAY.minusDays(1), "100", "100")), Collections.emptyList());

        assertEquals(2, state.days().size());
        assertEquals(TODAY, state.days().get(0).date());
        assertEquals(MONDAY, state.days().get(1).date());
    }

    @Test
    public void mediaUsaSoDiasComRegistroSemContarHoje() {
        HistoryState state = week(MONDAY, Arrays.asList(
                entry(TODAY, "100", "900"),
                entry(MONDAY, "100", "100"),
                entry(MONDAY.plusDays(1), "200", "100")), Collections.emptyList());

        assertEquals(2, state.average().days());
        assertEquals(0, new BigDecimal("150").compareTo(state.average().kcal()));
        assertEquals(0, new BigDecimal("15").compareTo(state.average().protein()));
        assertTrue(state.average().complete());
    }

    @Test
    public void semanaSoComHojeNaoTemMedia() {
        HistoryState state = week(MONDAY, Collections.singletonList(entry(TODAY, "100", "100")),
                Collections.emptyList());

        assertNull(state.average());
    }

    @Test
    public void valorDesconhecidoDeixaAMediaIncompleta() {
        HistoryState state = week(MONDAY, Collections.singletonList(entry(MONDAY, "100", null)),
                Collections.emptyList());

        assertFalse(state.average().complete());
    }

    @Test
    public void semanaPassadaContaTodosOsDiasEUsaAMetaDoDomingo() {
        LocalDate lastMonday = MONDAY.minusWeeks(1);
        List<Goal> goals = Arrays.asList(
                new Goal(lastMonday.minusDays(10), new BigDecimal("1800"), null, null, null),
                new Goal(lastMonday.plusDays(6), new BigDecimal("2000"), null, null, null),
                new Goal(TODAY, new BigDecimal("2200"), null, null, null));

        HistoryState state = week(lastMonday, Arrays.asList(
                entry(lastMonday, "100", "100"), entry(lastMonday.plusDays(6), "100", "300")), goals);

        assertEquals(2, state.average().days());
        assertEquals(0, new BigDecimal("2000").compareTo(state.goalKcal()));
        assertTrue(state.hasNextWeek());
        assertTrue(state.hasPreviousWeek());
        assertEquals(0, new BigDecimal("1800").compareTo(state.days().get(1).goal().kcal()));
    }

    @Test
    public void semanaAtualUsaAMetaDeHojeENaoTemProxima() {
        HistoryState state = week(MONDAY, Collections.emptyList(),
                Collections.singletonList(new Goal(TODAY, new BigDecimal("2000"), null, null, null)));

        assertEquals(0, new BigDecimal("2000").compareTo(state.goalKcal()));
        assertFalse(state.hasNextWeek());
        assertFalse(week(EARLIEST, Collections.emptyList(), Collections.emptyList()).hasPreviousWeek());
    }

    @Test
    public void sequenciaContaDiasSeguidosAteHoje() {
        HistoryState state = week(MONDAY, Arrays.asList(
                entry(TODAY, "100", "100"),
                entry(TODAY.minusDays(1), "100", "100"),
                entry(TODAY.minusDays(2), "100", "100"),
                entry(TODAY.minusDays(4), "100", "100")), Collections.emptyList());

        assertEquals(3, state.streak());
        assertTrue(state.streakIncludesToday());
    }

    @Test
    public void hojeVazioNaoQuebraASequenciaDeOntem() {
        HistoryState state = week(MONDAY, Arrays.asList(
                entry(TODAY.minusDays(1), "100", "100"),
                entry(TODAY.minusDays(2), "100", "100")), Collections.emptyList());

        assertEquals(2, state.streak());
        assertFalse(state.streakIncludesToday());
        assertEquals(0, week(MONDAY, Collections.singletonList(entry(TODAY.minusDays(2), "100", "100")),
                Collections.emptyList()).streak());
    }
}
