package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.domain.Food;

/**
 * Linha do editor de refeição. {@code gramsText} é o texto digitado (validado só ao salvar).
 * {@code itemId} existe quando a linha é um item já gravado num registro do diário.
 */
final class EditorItem {

    final String key = UUID.randomUUID().toString();
    final String itemId;
    final String foodId;
    final Food food;
    String gramsText;

    EditorItem(String itemId, String foodId, Food food, String gramsText) {
        this.itemId = itemId;
        this.foodId = foodId;
        this.food = food;
        this.gramsText = gramsText;
    }
}
