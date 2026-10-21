package io.github.lucaslimacelino_dev.calju.ui.diary;

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

public class DayStateBuilderTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    // Valores fictícios: só a aritmética importa.
    private static EntryWithItems entry(String id, Occasion occasion, long createdAt, String grams,
                                        String kcalPer100g, String proteinPer100g) {
        EntryWithItems entry = new EntryWithItems();
        entry.entry = new LogEntryEntity(id, DAY, occasion, null, null, createdAt, createdAt, null, 1, "PENDING");
        ItemWithNutrients item = new ItemWithNutrients();
        item.item = new LogItemEntity("item-" + id, id, "food", "Alimento " + id, null, null,
                new BigDecimal(grams), null, 0);
        item.nutrients = new ArrayList<>();
        if (kcalPer100g != null) {
            item.nutrients.add(new LogItemNutrientEntity(item.item.id, Nutrients.ENERGY,
                    new BigDecimal(kcalPer100g), "VALUE"));
        }
        if (proteinPer100g != null) {
            item.nutrients.add(new LogItemNutrientEntity(item.item.id, Nutrients.PROTEIN,
                    new BigDecimal(proteinPer100g), "VALUE"));
        }
        entry.items = Collections.singletonList(item);
        return entry;
    }

    private static DayState build(List<EntryWithItems> entries) {
        return DayStateBuilder.build(DAY, entries, Collections.emptyList());
    }

    @Test
    public void usaAMetaVigenteNoDia() {
        List<Goal> goals = Arrays.asList(
                new Goal(DAY.minusDays(5), new BigDecimal("1000"), null, null, null),
                new Goal(DAY.plusDays(1), new BigDecimal("3000"), null, null, null));

        DayState state = DayStateBuilder.build(DAY, Collections.emptyList(), goals);

        assertEquals(0, new BigDecimal("1000").compareTo(state.goal().kcal()));
    }

    @Test
    public void semMetaVigenteAMetaFicaNula() {
        assertNull(build(Collections.emptyList()).goal());
    }

    @Test
    public void somaOsTotaisDoDia() {
        DayState state = build(Arrays.asList(
                entry("a", Occasion.LUNCH, 2, "150", "165", "10"),
                entry("b", Occasion.BREAKFAST, 1, "50", "100", "20")));

        assertEquals(0, new BigDecimal("297.5").compareTo(state.kcal().amount()));
        assertEquals(0, new BigDecimal("25").compareTo(state.protein().amount()));
        assertTrue(state.kcal().complete());
    }

    @Test
    public void cartoesSeguemAOrdemDasOcasioesEDepoisOHorario() {
        DayState state = build(Arrays.asList(
                entry("jantar", Occasion.DINNER, 1, "10", "1", null),
                entry("almoco-2", Occasion.LUNCH, 3, "10", "1", null),
                entry("cafe", Occasion.BREAKFAST, 5, "10", "1", null),
                entry("almoco-1", Occasion.LUNCH, 2, "10", "1", null)));

        List<String> order = new ArrayList<>();
        for (EntryRow row : state.entries()) {
            order.add(row.entryId());
        }
        assertEquals(Arrays.asList("cafe", "almoco-1", "almoco-2", "jantar"), order);
    }

    @Test
    public void energiaDesconhecidaDeixaTotalIncompletoEOCartaoSemKcal() {
        DayState state = build(Arrays.asList(
                entry("a", Occasion.LUNCH, 1, "100", "200", null),
                entry("b", Occasion.LUNCH, 2, "100", null, null)));

        assertFalse(state.kcal().complete());
        assertEquals(0, new BigDecimal("200").compareTo(state.kcal().amount()));
        assertNull(state.entries().get(1).kcal());
    }

    @Test
    public void diaVazio() {
        DayState state = build(Collections.emptyList());

        assertEquals(0, BigDecimal.ZERO.compareTo(state.kcal().amount()));
        assertTrue(state.entries().isEmpty());
    }
}
