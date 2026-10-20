package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Optional;

/**
 * Texto digitado no campo de registro, já separado em busca, gramas e unidades.
 * {@code grams} e {@code units} são nulos quando não foram informados.
 */
public record FoodInput(String query, BigDecimal grams, BigDecimal units) {

    public FoodInput {
        Objects.requireNonNull(query, "query");
    }

    /**
     * Gramas a registrar para o alimento escolhido: as gramas digitadas; senão, as unidades
     * (1 se omitidas) vezes a porção padrão do alimento. Vazio quando o app precisa perguntar.
     */
    public Optional<BigDecimal> gramsFor(Food food) {
        Objects.requireNonNull(food, "food");
        if (grams != null) {
            return Optional.of(grams);
        }
        if (!food.portion().isPresent()) {
            return Optional.empty();
        }
        BigDecimal count = units != null ? units : BigDecimal.ONE;
        return Optional.of(food.portion().get().grams().multiply(count));
    }
}
