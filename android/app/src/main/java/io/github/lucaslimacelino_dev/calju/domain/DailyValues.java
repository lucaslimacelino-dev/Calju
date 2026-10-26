package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Nutrientes mostrados em "Nutrientes do dia", em grupos, com o valor diário de referência (VDR)
 * dos rótulos: ANVISA, Instrução Normativa nº 75/2020, Anexo II. O VDR fica na mesma unidade em
 * que a TACO guarda o nutriente (por isso o cobre, 900 µg no Anexo II, aparece como 0,9 mg).
 *
 * <p>Fora da lista: energia e macronutrientes (já estão no painel), umidade, cinzas, retinol e a
 * vitamina A em RE (a lista usa a coluna RAE da TACO, a mesma medida do Anexo II).
 * Vitamina B12: a TACO não traz; vem do USDA FoodData Central só para os alimentos comuns
 * (ver docs/fontes-de-dados.md). Niacina: a TACO traz a niacina pronta e o Anexo II usa NE (que
 * conta também o triptofano), então a porcentagem tende a sair menor que a real.
 */
public final class DailyValues {

    public enum Group {
        FIBER_AND_MINERALS,
        VITAMINS,
        /** Sódio e colesterol: o VDR funciona como limite, não como alvo. */
        WATCH
    }

    /** {@code dailyValue} nulo quando não há VDR comparável (hoje todos têm). */
    public record Reference(String code, Group group, NutrientUnit unit, BigDecimal dailyValue) {
    }

    public static final List<Reference> ALL = List.of(
            new Reference("FIBTG", Group.FIBER_AND_MINERALS, NutrientUnit.GRAM, new BigDecimal("25")),
            new Reference("CA", Group.FIBER_AND_MINERALS, NutrientUnit.MILLIGRAM, new BigDecimal("1000")),
            new Reference("FE", Group.FIBER_AND_MINERALS, NutrientUnit.MILLIGRAM, new BigDecimal("14")),
            new Reference("MG", Group.FIBER_AND_MINERALS, NutrientUnit.MILLIGRAM, new BigDecimal("420")),
            new Reference("K", Group.FIBER_AND_MINERALS, NutrientUnit.MILLIGRAM, new BigDecimal("3500")),
            new Reference("ZN", Group.FIBER_AND_MINERALS, NutrientUnit.MILLIGRAM, new BigDecimal("11")),
            new Reference("P", Group.FIBER_AND_MINERALS, NutrientUnit.MILLIGRAM, new BigDecimal("700")),
            new Reference("CU", Group.FIBER_AND_MINERALS, NutrientUnit.MILLIGRAM, new BigDecimal("0.9")),
            new Reference("MN", Group.FIBER_AND_MINERALS, NutrientUnit.MILLIGRAM, new BigDecimal("3")),
            new Reference("VITC", Group.VITAMINS, NutrientUnit.MILLIGRAM, new BigDecimal("100")),
            new Reference("VITA_RAE", Group.VITAMINS, NutrientUnit.MICROGRAM, new BigDecimal("800")),
            new Reference("THIA", Group.VITAMINS, NutrientUnit.MILLIGRAM, new BigDecimal("1.2")),
            new Reference("RIBF", Group.VITAMINS, NutrientUnit.MILLIGRAM, new BigDecimal("1.2")),
            new Reference("VITB6A", Group.VITAMINS, NutrientUnit.MILLIGRAM, new BigDecimal("1.3")),
            new Reference("NIA", Group.VITAMINS, NutrientUnit.MILLIGRAM, new BigDecimal("15")),
            new Reference("VITB12", Group.VITAMINS, NutrientUnit.MICROGRAM, new BigDecimal("2.4")),
            new Reference(Nutrients.SODIUM, Group.WATCH, NutrientUnit.MILLIGRAM, new BigDecimal("2000")),
            new Reference("CHOLE", Group.WATCH, NutrientUnit.MILLIGRAM, new BigDecimal("300")));

    private DailyValues() {
    }

    /** Porcentagem inteira do VDR (pode passar de 100). Nula se o nutriente não tem VDR. */
    public static Integer percent(Reference reference, BigDecimal amount) {
        if (reference.dailyValue() == null) {
            return null;
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(reference.dailyValue(), 0, RoundingMode.HALF_UP).intValueExact();
    }
}
