package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;

import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/**
 * Alimento aberto na tela de quantidade. Novo registro: {@code itemId} e {@code entryId} nulos.
 * Edição: ids do item existente e {@code food} é a cópia gravada no registro. {@code preferUnits}:
 * abrir mostrando a quantidade em porções ("4 unidades") em vez de gramas.
 */
public record SelectedFood(String foodId, Food food, BigDecimal suggestedGrams, boolean preferUnits,
                           Occasion occasion, String itemId, String entryId) {

    public boolean isEdit() {
        return itemId != null;
    }
}
