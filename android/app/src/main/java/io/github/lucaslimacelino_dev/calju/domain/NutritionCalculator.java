package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class NutritionCalculator {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private NutritionCalculator() {
    }

    /** consumido = valor por 100 g × quantidade em gramas ÷ 100, sem arredondar. */
    public static BigDecimal consumed(BigDecimal per100g, BigDecimal grams) {
        Objects.requireNonNull(per100g, "per100g");
        Objects.requireNonNull(grams, "grams");
        if (per100g.signum() < 0) {
            throw new IllegalArgumentException("Valor por 100 g não pode ser negativo: " + per100g);
        }
        if (grams.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero: " + grams);
        }
        return per100g.multiply(grams).divide(HUNDRED);
    }

    /**
     * Soma um nutriente em vários itens. Traço conta como zero; item sem o valor
     * conhecido não entra na soma e marca o total como incompleto.
     */
    public static NutrientTotal total(List<Intake> intakes, String nutrientCode) {
        Objects.requireNonNull(intakes, "intakes");
        Objects.requireNonNull(nutrientCode, "nutrientCode");

        BigDecimal sum = BigDecimal.ZERO;
        boolean complete = true;
        for (Intake intake : intakes) {
            Optional<NutrientAmount> amount = intake.food().nutrient(nutrientCode);
            if (!amount.isPresent()) {
                complete = false;
            } else if (amount.get().qualifier() == Qualifier.VALUE) {
                sum = sum.add(consumed(amount.get().amount(), intake.grams()));
            }
        }
        return new NutrientTotal(sum, complete);
    }
}
