package io.github.lucaslimacelino_dev.calju.ui.diary;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.github.lucaslimacelino_dev.calju.domain.Occasion;

public class OccasionSectionTest {

    private static EntryRow row(String id, Occasion occasion, String kcal) {
        return new EntryRow(id, occasion, 0L, "Registro " + id, 1, new BigDecimal("100"),
                kcal == null ? null : new BigDecimal(kcal), "item-" + id, null);
    }

    @Test
    public void diaVazioTemAsQuatroOcasioesNaOrdemDoDiario() {
        List<OccasionSection> sections = OccasionSection.group(Collections.emptyList());

        assertEquals(4, sections.size());
        assertEquals(Occasion.BREAKFAST, sections.get(0).occasion());
        assertEquals(Occasion.LUNCH, sections.get(1).occasion());
        assertEquals(Occasion.DINNER, sections.get(2).occasion());
        assertEquals(Occasion.SNACK, sections.get(3).occasion());
        for (OccasionSection section : sections) {
            assertTrue(section.isEmpty());
            assertTrue(section.complete());
            assertEquals(0, BigDecimal.ZERO.compareTo(section.kcal()));
        }
    }

    @Test
    public void somaAsKcalDosRegistrosDeCadaOcasiao() {
        List<OccasionSection> sections = OccasionSection.group(Arrays.asList(
                row("a", Occasion.LUNCH, "300"), row("b", Occasion.BREAKFAST, "120"),
                row("c", Occasion.LUNCH, "150.5")));

        assertEquals(1, sections.get(0).entries().size());
        assertEquals(0, new BigDecimal("120").compareTo(sections.get(0).kcal()));
        assertEquals(2, sections.get(1).entries().size());
        assertEquals("a", sections.get(1).entries().get(0).entryId());
        assertEquals("c", sections.get(1).entries().get(1).entryId());
        assertEquals(0, new BigDecimal("450.5").compareTo(sections.get(1).kcal()));
        assertTrue(sections.get(2).isEmpty());
    }

    @Test
    public void energiaDesconhecidaDeixaOTotalIncompletoSemVirarZero() {
        List<OccasionSection> sections = OccasionSection.group(Arrays.asList(
                row("a", Occasion.DINNER, "200"), row("b", Occasion.DINNER, null)));

        OccasionSection dinner = sections.get(2);
        assertFalse(dinner.complete());
        assertEquals(0, new BigDecimal("200").compareTo(dinner.kcal()));
        assertEquals(2, dinner.entries().size());
    }
}
