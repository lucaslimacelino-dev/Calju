package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;

/**
 * Item da lista de sugestões. {@code grams} e {@code kcal} são nulos quando o texto não permite
 * saber a quantidade; {@code portionText} só existe quando as gramas vieram da porção padrão.
 */
public record Suggestion(String foodId, String name, BigDecimal grams, BigDecimal kcal,
                         String portionText, BigDecimal kcalPer100g) {
}
