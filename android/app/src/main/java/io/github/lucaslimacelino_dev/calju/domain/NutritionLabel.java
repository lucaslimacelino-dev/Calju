package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Valores da tabela nutricional como o usuário leu na embalagem, referentes a {@code servingGrams}
 * gramas (100 g ou o peso da porção). Fibra e sódio são opcionais: nulo = desconhecido, não zero.
 */
public record NutritionLabel(BigDecimal servingGrams, BigDecimal kcal, BigDecimal proteinG,
                             BigDecimal carbohydrateG, BigDecimal fatG, BigDecimal fiberG, BigDecimal sodiumMg) {

    private static final BigDecimal HUNDRED = new BigDecimal("100");
    /** Folga para o arredondamento dos rótulos ao comparar a soma dos macros com o peso. */
    private static final BigDecimal ROUNDING_TOLERANCE_G = BigDecimal.ONE;

    public enum Problem {
        SERVING_NOT_POSITIVE,
        KCAL_MISSING,
        PROTEIN_MISSING,
        CARBOHYDRATE_MISSING,
        FAT_MISSING,
        NEGATIVE_VALUE,
        /** Proteína + carboidrato + gordura pesam mais que a própria porção: provável erro de digitação. */
        MACROS_EXCEED_WEIGHT
    }

    public Set<Problem> validate() {
        Set<Problem> problems = EnumSet.noneOf(Problem.class);
        if (servingGrams == null || servingGrams.signum() <= 0) {
            problems.add(Problem.SERVING_NOT_POSITIVE);
        }
        if (kcal == null) {
            problems.add(Problem.KCAL_MISSING);
        }
        if (proteinG == null) {
            problems.add(Problem.PROTEIN_MISSING);
        }
        if (carbohydrateG == null) {
            problems.add(Problem.CARBOHYDRATE_MISSING);
        }
        if (fatG == null) {
            problems.add(Problem.FAT_MISSING);
        }
        for (BigDecimal value : new BigDecimal[]{kcal, proteinG, carbohydrateG, fatG, fiberG, sodiumMg}) {
            if (value != null && value.signum() < 0) {
                problems.add(Problem.NEGATIVE_VALUE);
            }
        }
        if (problems.isEmpty()
                && proteinG.add(carbohydrateG).add(fatG).compareTo(servingGrams.add(ROUNDING_TOLERANCE_G)) > 0) {
            problems.add(Problem.MACROS_EXCEED_WEIGHT);
        }
        return problems;
    }

    /** Valores por 100 g, indexados pelo código INFOODS; opcionais ausentes ficam fora do mapa. */
    public Map<String, BigDecimal> per100g() {
        if (!validate().isEmpty()) {
            throw new IllegalStateException("Rótulo inválido: " + validate());
        }
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put(Nutrients.ENERGY, scale(kcal));
        values.put(Nutrients.PROTEIN, scale(proteinG));
        values.put(Nutrients.CARBOHYDRATE, scale(carbohydrateG));
        values.put(Nutrients.FAT, scale(fatG));
        if (fiberG != null) {
            values.put(Nutrients.FIBER, scale(fiberG));
        }
        if (sodiumMg != null) {
            values.put(Nutrients.SODIUM, scale(sodiumMg));
        }
        return values;
    }

    private BigDecimal scale(BigDecimal value) {
        BigDecimal result = value.multiply(HUNDRED).divide(servingGrams, 4, RoundingMode.HALF_UP).stripTrailingZeros();
        return result.scale() < 0 ? result.setScale(0) : result;
    }
}
