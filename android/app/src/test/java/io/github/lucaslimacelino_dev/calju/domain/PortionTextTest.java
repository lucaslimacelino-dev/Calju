package io.github.lucaslimacelino_dev.calju.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;

import org.junit.Test;

public class PortionTextTest {

    private static BigDecimal n(String value) {
        return new BigDecimal(value);
    }

    @Test
    public void contaPorcoesRedondas() {
        assertEquals(n("4"), PortionText.count(n("300"), n("75")));
        assertEquals(n("1.5"), PortionText.count(n("75"), n("50")));
        assertEquals(n("1"), PortionText.count(n("45.0000"), n("45")));
    }

    @Test
    public void quantidadeQueNaoDaPorcaoRedondaFicaEmGramas() {
        assertNull(PortionText.count(n("100"), n("75")));
        assertNull(PortionText.count(n("150"), null));
        assertNull(PortionText.count(n("10"), n("75")));
    }

    @Test
    public void porcoesExatasParaConverterGramas() {
        assertEquals(n("1.33"), PortionText.exactCount(n("100"), n("75")));
        assertEquals(n("2"), PortionText.exactCount(n("150"), n("75")));
    }

    @Test
    public void rotuloNoSingularENoPlural() {
        assertEquals("unidade média", PortionText.noun("1 unidade média", n("1")));
        assertEquals("unidade", PortionText.noun("1 unidade", n("0.5")));
        assertEquals("unidades médias", PortionText.noun("1 unidade média", n("4")));
        assertEquals("unidades grandes", PortionText.noun("1 unidade grande", n("2")));
        assertEquals("colheres de sopa", PortionText.noun("1 colher de sopa", n("3")));
        assertEquals("xícaras", PortionText.noun("1 xícara", n("1.5")));
        assertEquals("copos (200 ml)", PortionText.noun("1 copo (200 ml)", n("2")));
    }
}
