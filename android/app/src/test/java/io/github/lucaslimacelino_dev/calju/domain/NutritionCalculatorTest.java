package io.github.lucaslimacelino_dev.calju.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class NutritionCalculatorTest {

    // Alimentos fictícios: os valores servem só para testar a aritmética.
    private static Food food(String name, Object... codeAndAmount) {
        Map<String, NutrientAmount> nutrients = new HashMap<>();
        for (int i = 0; i < codeAndAmount.length; i += 2) {
            nutrients.put((String) codeAndAmount[i], (NutrientAmount) codeAndAmount[i + 1]);
        }
        return new Food(name, nutrients, null);
    }

    private static void assertSameValue(String expected, BigDecimal actual) {
        assertEquals("esperado " + expected + ", obtido " + actual, 0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    public void exemploDoPlano_165por100g_vezes150g_da247virgula5() {
        assertSameValue("247.5", NutritionCalculator.consumed(new BigDecimal("165"), new BigDecimal("150")));
    }

    @Test
    public void valorZeroMedidoResultaEmZero() {
        assertSameValue("0", NutritionCalculator.consumed(BigDecimal.ZERO, new BigDecimal("80")));
    }

    @Test
    public void naoArredondaNoMeioDoCalculo() {
        assertSameValue("0.0333", NutritionCalculator.consumed(new BigDecimal("3.33"), BigDecimal.ONE));
    }

    @Test
    public void quantidadeZeroOuNegativaEhRejeitada() {
        assertThrows(IllegalArgumentException.class,
                () -> NutritionCalculator.consumed(new BigDecimal("100"), BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> NutritionCalculator.consumed(new BigDecimal("100"), new BigDecimal("-10")));
    }

    @Test
    public void valorPor100gNegativoEhRejeitado() {
        assertThrows(IllegalArgumentException.class,
                () -> NutritionCalculator.consumed(new BigDecimal("-1"), new BigDecimal("100")));
    }

    @Test
    public void valoresNulosSaoRejeitados() {
        assertThrows(NullPointerException.class, () -> NutritionCalculator.consumed(null, BigDecimal.ONE));
        assertThrows(NullPointerException.class, () -> NutritionCalculator.consumed(BigDecimal.ONE, null));
    }

    @Test
    public void somaVariosItensSemArredondar() {
        Food a = food("Alimento A", Nutrients.ENERGY, NutrientAmount.of("165"));
        Food b = food("Alimento B", Nutrients.ENERGY, NutrientAmount.of("33.3"));
        List<Intake> day = Arrays.asList(
                new Intake(a, new BigDecimal("150")),
                new Intake(b, new BigDecimal("50")));

        NutrientTotal total = NutritionCalculator.total(day, Nutrients.ENERGY);

        assertSameValue("264.15", total.amount());
        assertTrue(total.complete());
    }

    @Test
    public void nutrienteDesconhecidoNaoViraZeroEMarcaTotalIncompleto() {
        Food conhecido = food("Alimento A", Nutrients.FAT, NutrientAmount.of("10"));
        Food semGordura = food("Alimento B");
        List<Intake> day = Arrays.asList(
                new Intake(conhecido, new BigDecimal("200")),
                new Intake(semGordura, new BigDecimal("100")));

        NutrientTotal total = NutritionCalculator.total(day, Nutrients.FAT);

        assertSameValue("20", total.amount());
        assertFalse(total.complete());
    }

    @Test
    public void tracoContaComoZeroEMantemTotalCompleto() {
        Food comTraco = food("Alimento A", Nutrients.FAT, NutrientAmount.trace());
        Food comValor = food("Alimento B", Nutrients.FAT, NutrientAmount.of("2"));
        List<Intake> day = Arrays.asList(
                new Intake(comTraco, new BigDecimal("100")),
                new Intake(comValor, new BigDecimal("50")));

        NutrientTotal total = NutritionCalculator.total(day, Nutrients.FAT);

        assertSameValue("1", total.amount());
        assertTrue(total.complete());
    }

    @Test
    public void diaVazioTotalZeroCompleto() {
        NutrientTotal total = NutritionCalculator.total(Collections.emptyList(), Nutrients.PROTEIN);

        assertSameValue("0", total.amount());
        assertTrue(total.complete());
    }

    @Test
    public void quantidadeNegativaNoNutrienteEhRejeitada() {
        assertThrows(IllegalArgumentException.class, () -> NutrientAmount.of("-0.1"));
    }

    @Test
    public void registroComQuantidadeZeroEhRejeitado() {
        Food a = food("Alimento A");
        assertThrows(IllegalArgumentException.class, () -> new Intake(a, BigDecimal.ZERO));
    }

    @Test
    public void arredondamentoSoNaExibicao() {
        assertSameValue("248", NutrientUnit.KCAL.roundForDisplay(new BigDecimal("247.5")));
        assertSameValue("2.3", NutrientUnit.GRAM.roundForDisplay(new BigDecimal("2.25")));
        assertSameValue("13", NutrientUnit.MILLIGRAM.roundForDisplay(new BigDecimal("12.5")));
    }

    @Test
    public void miligramasPequenosMantemUmaCasa() {
        assertSameValue("0.2", NutrientUnit.MILLIGRAM.roundForDisplay(new BigDecimal("0.24")));
        assertSameValue("9.9", NutrientUnit.MICROGRAM.roundForDisplay(new BigDecimal("9.94")));
        assertSameValue("10", NutrientUnit.MILLIGRAM.roundForDisplay(new BigDecimal("9.96")));
    }
}
