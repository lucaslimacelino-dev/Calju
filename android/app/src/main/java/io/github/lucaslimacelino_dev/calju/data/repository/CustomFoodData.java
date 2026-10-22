package io.github.lucaslimacelino_dev.calju.data.repository;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Alimento do usuário como o formulário o grava e o lê: valores já por 100 g (códigos INFOODS)
 * e porção opcional ({@code portionLabel} e {@code portionGrams} juntos ou ambos nulos).
 */
public record CustomFoodData(String name, String brand, Map<String, BigDecimal> per100g,
                             String portionLabel, BigDecimal portionGrams) {
}
