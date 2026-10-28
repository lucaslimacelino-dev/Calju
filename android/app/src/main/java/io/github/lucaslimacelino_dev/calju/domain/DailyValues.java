package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Sex;

/**
 * Nutrientes mostrados em "Nutrientes do dia", em grupos, com o valor diário de referência.
 *
 * <ul>
 *   <li>Com o perfil do questionário (adulto de 19 a 80 anos, sem gravidez, amamentação ou doença
 *       com dieta orientada): Dietary Reference Intakes das National Academies (RDA ou AI por sexo e
 *       idade; sódio = CDRR de 2019, um limite), como publicadas nas tabelas da Health Canada
 *       (conferidas em 27/10/2026). Fibras: 14 g a cada 1.000 kcal da meta, a mesma base da AI.</li>
 *   <li>Sem perfil adulto: valores dos rótulos da ANVISA (Instrução Normativa nº 75/2020, Anexo II).</li>
 *   <li>Limites do dia (grupo {@link Group#WATCH}): sódio (acima) e gordura total, no máximo 30% das
 *       calorias da meta (OMS, ficha "Healthy diet" e diretriz de 2023); sem meta, os 65 g da ANVISA.
 *       O colesterol saiu da lista: as DRIs não definem valor para ele.</li>
 * </ul>
 *
 * <p>Os valores ficam na mesma unidade em que a TACO guarda o nutriente (cobre, 900 µg, aparece como
 * 0,9 mg). Fora da lista: energia e macronutrientes (já estão no painel), umidade, cinzas, retinol e
 * vitamina A em RE (a lista usa a coluna RAE da TACO). Vitamina B12: vem do USDA FoodData Central
 * só para os alimentos comuns (ver docs/fontes-de-dados.md). Niacina: a TACO traz a niacina pronta e
 * as referências usam NE (que conta também o triptofano), então a porcentagem tende a sair menor.
 */
public final class DailyValues {

    public enum Group {
        FIBER_AND_MINERALS,
        VITAMINS,
        /** Limites: o ideal é ficar abaixo de 100%. */
        WATCH
    }

    /** {@code dailyValue} nulo quando não há referência. */
    public record Reference(String code, Group group, NutrientUnit unit, BigDecimal dailyValue) {
    }

    /** Idades em que valem as DRIs de adulto usadas aqui (a faixa de 14 a 18 anos tem outros valores). */
    static final int ADULT_MIN_AGE = 19;
    static final int ADULT_MAX_AGE = 80;

    private static final BigDecimal FIBER_PER_1000_KCAL = new BigDecimal("14");
    private static final BigDecimal FAT_MAX_SHARE = new BigDecimal("0.30");
    private static final BigDecimal KCAL_PER_G_FAT = new BigDecimal("9");

    /** Valores dos rótulos (ANVISA, IN 75/2020, Anexo II), na ordem em que aparecem na tela. */
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
            new Reference(Nutrients.FAT, Group.WATCH, NutrientUnit.GRAM, new BigDecimal("65")));

    private DailyValues() {
    }

    /** Sódio e gordura total: passar de 100% é exceder o limite, e não atingir a meta. */
    public static boolean isLimit(Reference reference) {
        return reference.group() == Group.WATCH;
    }

    /**
     * Referências para a pessoa. Sem perfil, fora de 19 a 80 anos ou com situação especial, usa os
     * valores da ANVISA. {@code goalKcal} nula: fibras pela AI da idade e gordura pelos 65 g da ANVISA.
     */
    public static List<Reference> forPerson(UserProfile profile, LocalDate day, BigDecimal goalKcal) {
        boolean adult = profile != null && !profile.specialCondition()
                && profile.age(day) >= ADULT_MIN_AGE && profile.age(day) <= ADULT_MAX_AGE;
        if (!adult && goalKcal == null) {
            return ALL;
        }
        List<Reference> references = new ArrayList<>();
        for (Reference reference : ALL) {
            BigDecimal value = adult
                    ? dri(reference.code(), profile.sex() == Sex.MALE, profile.age(day), goalKcal)
                    : reference.dailyValue();
            if (reference.code().equals(Nutrients.FAT) && goalKcal != null && goalKcal.signum() > 0) {
                value = goalKcal.multiply(FAT_MAX_SHARE).divide(KCAL_PER_G_FAT, 1, RoundingMode.HALF_UP);
            }
            references.add(new Reference(reference.code(), reference.group(), reference.unit(), value));
        }
        return references;
    }

    private static BigDecimal dri(String code, boolean male, int age, BigDecimal goalKcal) {
        return switch (code) {
            case "FIBTG" -> goalKcal != null && goalKcal.signum() > 0
                    ? goalKcal.multiply(FIBER_PER_1000_KCAL).divide(BigDecimal.valueOf(1000), 1, RoundingMode.HALF_UP)
                    : value(male ? (age <= 50 ? "38" : "30") : (age <= 50 ? "25" : "21"));
            case "CA" -> value((male ? age <= 70 : age <= 50) ? "1000" : "1200");
            case "FE" -> value(male || age > 50 ? "8" : "18");
            case "MG" -> value(male ? (age <= 30 ? "400" : "420") : (age <= 30 ? "310" : "320"));
            case "K" -> value(male ? "3400" : "2600");
            case "ZN" -> value(male ? "11" : "8");
            case "P" -> value("700");
            case "CU" -> value("0.9");
            case "MN" -> value(male ? "2.3" : "1.8");
            case "VITC" -> value(male ? "90" : "75");
            case "VITA_RAE" -> value(male ? "900" : "700");
            case "THIA" -> value(male ? "1.2" : "1.1");
            case "RIBF" -> value(male ? "1.3" : "1.1");
            case "VITB6A" -> value(age <= 50 ? "1.3" : male ? "1.7" : "1.5");
            case "NIA" -> value(male ? "16" : "14");
            case "VITB12" -> value("2.4");
            case Nutrients.SODIUM -> value("2300");
            case Nutrients.FAT -> value("65");
            default -> throw new IllegalArgumentException("Nutriente sem referência: " + code);
        };
    }

    private static BigDecimal value(String text) {
        return new BigDecimal(text);
    }

    /** Porcentagem inteira da referência (pode passar de 100). Nula se o nutriente não tem referência. */
    public static Integer percent(Reference reference, BigDecimal amount) {
        if (reference.dailyValue() == null) {
            return null;
        }
        return amount.multiply(BigDecimal.valueOf(100))
                .divide(reference.dailyValue(), 0, RoundingMode.HALF_UP).intValueExact();
    }
}
