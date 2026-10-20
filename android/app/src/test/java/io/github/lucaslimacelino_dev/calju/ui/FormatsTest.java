package io.github.lucaslimacelino_dev.calju.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.math.BigDecimal;

import org.junit.Test;

import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;

public class FormatsTest {

    @Test
    public void gramasSemCasasQuandoInteiro() {
        assertEquals("150 g", Formats.grams(new BigDecimal("150")));
        assertEquals("1.200 g", Formats.grams(new BigDecimal("1200")));
    }

    @Test
    public void gramasComVirgulaDecimal() {
        assertEquals("37,5 g", Formats.grams(new BigDecimal("37.5")));
        assertEquals("33,3 g", Formats.grams(new BigDecimal("33.33")));
    }

    @Test
    public void quantidadeDeUnidades() {
        assertEquals("2", Formats.count(new BigDecimal("2")));
        assertEquals("1,5", Formats.count(new BigDecimal("1.5")));
    }

    @Test
    public void kcalInteiroArredondadoParaCima() {
        assertEquals("248", Formats.amount(new BigDecimal("247.5"), NutrientUnit.KCAL));
        assertEquals("1.240", Formats.amount(new BigDecimal("1240.4"), NutrientUnit.KCAL));
    }

    @Test
    public void macroComUmaCasa() {
        assertEquals("2,3", Formats.amount(new BigDecimal("2.25"), NutrientUnit.GRAM));
        assertEquals("0,0", Formats.amount(BigDecimal.ZERO, NutrientUnit.GRAM));
    }

    @Test
    public void leNumeroDigitadoComVirgulaOuPonto() {
        assertEquals(new BigDecimal("37.5"), Formats.parsePositive("37,5"));
        assertEquals(new BigDecimal("37.5"), Formats.parsePositive(" 37.5 "));
    }

    @Test
    public void textoInvalidoOuNaoPositivoViraNulo() {
        assertNull(Formats.parsePositive(""));
        assertNull(Formats.parsePositive("abc"));
        assertNull(Formats.parsePositive("0"));
        assertNull(Formats.parsePositive("-5"));
    }
}
