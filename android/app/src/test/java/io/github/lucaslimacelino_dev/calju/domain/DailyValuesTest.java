package io.github.lucaslimacelino_dev.calju.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import io.github.lucaslimacelino_dev.calju.domain.DailyValues.Reference;

public class DailyValuesTest {

    private static Reference reference(String code) {
        for (Reference reference : DailyValues.ALL) {
            if (reference.code().equals(code)) {
                return reference;
            }
        }
        throw new AssertionError(code);
    }

    @Test
    public void porcentagemDoValorDiario() {
        assertEquals(Integer.valueOf(50), DailyValues.percent(reference("FE"), new BigDecimal("7")));
        assertEquals(Integer.valueOf(130), DailyValues.percent(reference("NA"), new BigDecimal("2600")));
        assertEquals(Integer.valueOf(0), DailyValues.percent(reference("CA"), BigDecimal.ZERO));
    }

    @Test
    public void cobreUsaOValorDoAnexoEmMiligramas() {
        // 900 µg no Anexo II = 0,9 mg, a unidade da TACO.
        assertEquals(Integer.valueOf(100), DailyValues.percent(reference("CU"), new BigDecimal("0.9")));
    }

    @Test
    public void vitaminaAEmRaeEB12TemPorcentagem() {
        assertEquals(Integer.valueOf(50), DailyValues.percent(reference("VITA_RAE"), new BigDecimal("400")));
        assertEquals(Integer.valueOf(50), DailyValues.percent(reference("VITB12"), new BigDecimal("1.2")));
    }

    @Test
    public void listaNaoTemMacrosUmidadeCinzasRetinolNemVitaminaAEmRe() {
        Set<String> codes = new HashSet<>();
        for (Reference reference : DailyValues.ALL) {
            assertTrue("repetido: " + reference.code(), codes.add(reference.code()));
        }
        for (String removed : new String[]{"ENERC", "PROCNT", "CHOCDF", "FAT", "WATER", "ASH", "RETOL", "VITA"}) {
            assertFalse(removed, codes.contains(removed));
        }
        assertEquals(18, codes.size());
    }
}
