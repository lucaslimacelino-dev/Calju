package io.github.lucaslimacelino_dev.calju.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class GoalTest {

    // Metas fictícias: o teste só verifica qual meta vale em cada dia.
    private static Goal goal(String from, String kcal) {
        return new Goal(LocalDate.parse(from), new BigDecimal(kcal), null, null, null);
    }

    private static final List<Goal> GOALS = Arrays.asList(
            goal("2026-10-10", "1000"),
            goal("2026-10-20", "2000"),
            goal("2026-10-15", "1500"));

    @Test
    public void vaiAMetaDeVigenciaMaisRecenteAteODia() {
        assertEquals(new BigDecimal("1000"), Goal.forDate(GOALS, LocalDate.parse("2026-10-12")).get().kcal());
        assertEquals(new BigDecimal("1500"), Goal.forDate(GOALS, LocalDate.parse("2026-10-19")).get().kcal());
        assertEquals(new BigDecimal("2000"), Goal.forDate(GOALS, LocalDate.parse("2026-10-20")).get().kcal());
        assertEquals(new BigDecimal("2000"), Goal.forDate(GOALS, LocalDate.parse("2026-12-01")).get().kcal());
    }

    @Test
    public void diaAnteriorATodasAsMetasFicaSemMeta() {
        assertFalse(Goal.forDate(GOALS, LocalDate.parse("2026-10-09")).isPresent());
        assertFalse(Goal.forDate(Collections.emptyList(), LocalDate.parse("2026-10-20")).isPresent());
    }

    @Test
    public void kcalEhObrigatoriaEPositiva() {
        assertThrows(NullPointerException.class,
                () -> new Goal(LocalDate.parse("2026-10-20"), null, null, null, null));
        assertThrows(IllegalArgumentException.class, () -> goal("2026-10-20", "0"));
    }

    @Test
    public void metaDeMacroOpcionalMasPositivaSeInformada() {
        new Goal(LocalDate.parse("2026-10-20"), BigDecimal.TEN, null, null, null);
        assertThrows(IllegalArgumentException.class,
                () -> new Goal(LocalDate.parse("2026-10-20"), BigDecimal.TEN, BigDecimal.ZERO, null, null));
    }
}
