package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;

/** Refeição salva pronta para a lista: itens resumidos e kcal (nula se alguma for desconhecida). */
public record MealSummary(String mealId, String name, String itemsText, BigDecimal kcal) {
}
