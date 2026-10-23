package io.github.lucaslimacelino_dev.calju.data.repository;

import java.math.BigDecimal;
import java.util.Map;

import io.github.lucaslimacelino_dev.calju.domain.QuantityUnit;

/**
 * Alimento do usuário como o formulário o grava e o lê: valores já por 100 g ou 100 ml (códigos
 * INFOODS) e porção opcional ({@code portionLabel} e {@code portionGrams} juntos ou ambos nulos;
 * {@code portionGrams} na mesma unidade do alimento).
 */
public record CustomFoodData(String name, String brand, Map<String, BigDecimal> per100g,
                             String portionLabel, BigDecimal portionGrams, QuantityUnit unit) {

    public CustomFoodData {
        unit = unit == null ? QuantityUnit.GRAM : unit;
    }

    /** Alimento medido em gramas. */
    public CustomFoodData(String name, String brand, Map<String, BigDecimal> per100g, String portionLabel,
                          BigDecimal portionGrams) {
        this(name, brand, per100g, portionLabel, portionGrams, QuantityUnit.GRAM);
    }
}
