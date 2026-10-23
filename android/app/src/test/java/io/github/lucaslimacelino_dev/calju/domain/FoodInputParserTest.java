package io.github.lucaslimacelino_dev.calju.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.math.BigDecimal;
import java.util.Collections;

import org.junit.Test;

public class FoodInputParserTest {

    private static void assertSameValue(String expected, BigDecimal actual) {
        assertEquals("esperado " + expected + ", obtido " + actual, 0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    public void nomeSeguidoDeNumeroSaoGramas() {
        FoodInput input = FoodInputParser.parse("arroz 150");
        assertEquals("arroz", input.query());
        assertSameValue("150", input.grams());
        assertNull(input.units());
    }

    @Test
    public void aceitaUnidadeDeGramasColadaOuSeparada() {
        assertSameValue("150", FoodInputParser.parse("arroz 150g").grams());
        assertSameValue("150", FoodInputParser.parse("arroz 150 g").grams());
        assertSameValue("150", FoodInputParser.parse("arroz 150 gramas").grams());
    }

    @Test
    public void mililitrosContamComoGramas() {
        assertSameValue("200", FoodInputParser.parse("suco 200 ml").grams());
        assertSameValue("200", FoodInputParser.parse("suco 200ml").grams());
        FoodInput input = FoodInputParser.parse("200 ml de suco");
        assertEquals("suco", input.query());
        assertSameValue("200", input.grams());
    }

    @Test
    public void gramasAntesDoNome() {
        FoodInput input = FoodInputParser.parse("150 g de arroz");
        assertEquals("arroz", input.query());
        assertSameValue("150", input.grams());

        assertSameValue("80", FoodInputParser.parse("80g feijão").grams());
    }

    @Test
    public void nomeComVariasPalavras() {
        FoodInput input = FoodInputParser.parse("pão francês 50");
        assertEquals("pão francês", input.query());
        assertSameValue("50", input.grams());
    }

    @Test
    public void numeroAntesDoNomeSaoUnidades() {
        FoodInput input = FoodInputParser.parse("2 bananas");
        assertEquals("bananas", input.query());
        assertSameValue("2", input.units());
        assertNull(input.grams());
    }

    @Test
    public void aceitaVirgulaDecimal() {
        assertSameValue("1.5", FoodInputParser.parse("1,5 banana").units());
        assertSameValue("12.5", FoodInputParser.parse("azeite 12,5").grams());
    }

    @Test
    public void semQuantidade() {
        FoodInput input = FoodInputParser.parse("  Banana   prata  ");
        assertEquals("Banana prata", input.query());
        assertNull(input.grams());
        assertNull(input.units());
    }

    @Test
    public void quantidadeZeroEhIgnorada() {
        FoodInput input = FoodInputParser.parse("arroz 0");
        assertEquals("arroz", input.query());
        assertNull(input.grams());
    }

    @Test
    public void textoVazio() {
        FoodInput input = FoodInputParser.parse("   ");
        assertEquals("", input.query());
        assertNull(input.grams());
        assertNull(input.units());
    }

    @Test
    public void textoNuloEhRejeitado() {
        assertThrows(NullPointerException.class, () -> FoodInputParser.parse(null));
    }

    // Porção fictícia: o peso real virá da tabela do IBGE na Fase 4a.
    private static final Food COM_PORCAO = new Food("Fruta A", Collections.emptyMap(),
            new Portion("1 unidade", new BigDecimal("100"), PortionSource.REFERENCE));
    private static final Food SEM_PORCAO = new Food("Cereal A", Collections.emptyMap(), null);

    @Test
    public void semQuantidadeUsaUmaPorcaoPadrao() {
        assertSameValue("100", FoodInputParser.parse("fruta").gramsFor(COM_PORCAO).get());
    }

    @Test
    public void unidadesMultiplicamAPorcaoPadrao() {
        assertSameValue("250", FoodInputParser.parse("2,5 frutas").gramsFor(COM_PORCAO).get());
    }

    @Test
    public void gramasDigitadasTemPrioridadeSobreAPorcao() {
        assertSameValue("120", FoodInputParser.parse("fruta 120").gramsFor(COM_PORCAO).get());
        assertSameValue("90", FoodInputParser.parse("2 frutas 90").gramsFor(COM_PORCAO).get());
    }

    @Test
    public void semPorcaoESemGramasOAppPrecisaPerguntar() {
        assertFalse(FoodInputParser.parse("cereal").gramsFor(SEM_PORCAO).isPresent());
        assertFalse(FoodInputParser.parse("2 cereais").gramsFor(SEM_PORCAO).isPresent());
    }

    @Test
    public void porcaoComPesoZeroEhRejeitada() {
        assertThrows(IllegalArgumentException.class,
                () -> new Portion("1 unidade", BigDecimal.ZERO, PortionSource.USER));
    }
}
