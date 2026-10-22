package io.github.lucaslimacelino_dev.calju.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.Map;

import org.junit.Test;

public class NutritionLabelTest {

    private static BigDecimal d(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    // Valores fictícios de rótulo: só a aritmética importa.
    private static NutritionLabel label(String serving, String kcal, String protein, String carbohydrate,
                                        String fat, String fiber, String sodium) {
        return new NutritionLabel(d(serving), d(kcal), d(protein), d(carbohydrate), d(fat), d(fiber), d(sodium));
    }

    @Test
    public void valoresPorPorcaoSaoConvertidosPara100g() {
        Map<String, BigDecimal> per100g = label("30", "120", "2.5", "21", "3", "1.2", "45").per100g();

        assertEquals(d("400"), per100g.get(Nutrients.ENERGY));
        assertEquals(d("8.3333"), per100g.get(Nutrients.PROTEIN));
        assertEquals(d("70"), per100g.get(Nutrients.CARBOHYDRATE));
        assertEquals(d("10"), per100g.get(Nutrients.FAT));
        assertEquals(d("4"), per100g.get(Nutrients.FIBER));
        assertEquals(d("150"), per100g.get(Nutrients.SODIUM));
    }

    @Test
    public void valoresJaPor100gNaoMudam() {
        Map<String, BigDecimal> per100g = label("100", "52", "0.3", "13.8", "0.2", null, null).per100g();

        assertEquals(d("52"), per100g.get(Nutrients.ENERGY));
        assertEquals(d("0.3"), per100g.get(Nutrients.PROTEIN));
        assertEquals(d("13.8"), per100g.get(Nutrients.CARBOHYDRATE));
    }

    @Test
    public void opcionaisVaziosFicamDesconhecidosEZeroContinuaZero() {
        Map<String, BigDecimal> per100g = label("50", "0", "0", "0", "0", null, "0").per100g();

        assertFalse(per100g.containsKey(Nutrients.FIBER));
        assertEquals(0, BigDecimal.ZERO.compareTo(per100g.get(Nutrients.SODIUM)));
        assertEquals("0", per100g.get(Nutrients.ENERGY).toPlainString());
    }

    @Test
    public void camposObrigatoriosEPorcaoInvalida() {
        assertEquals(EnumSet.of(NutritionLabel.Problem.SERVING_NOT_POSITIVE, NutritionLabel.Problem.KCAL_MISSING,
                        NutritionLabel.Problem.PROTEIN_MISSING, NutritionLabel.Problem.CARBOHYDRATE_MISSING,
                        NutritionLabel.Problem.FAT_MISSING),
                label("0", null, null, null, null, null, null).validate());
        assertTrue(label("30", "-1", "1", "1", "1", null, null).validate()
                .contains(NutritionLabel.Problem.NEGATIVE_VALUE));
    }

    @Test
    public void macrosMaisPesadosQueAPorcaoSaoRecusadosComFolgaDeArredondamento() {
        assertEquals(EnumSet.of(NutritionLabel.Problem.MACROS_EXCEED_WEIGHT),
                label("30", "200", "10", "15", "8", null, null).validate());
        assertTrue(label("10", "90", "0", "0", "10.5", null, null).validate().isEmpty());
    }

    @Test(expected = IllegalStateException.class)
    public void converterRotuloInvalidoEhErro() {
        label("0", "1", "1", "1", "1", null, null).per100g();
    }
}
