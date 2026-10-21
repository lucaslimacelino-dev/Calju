package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;

import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/**
 * Um lançamento do dia pronto para exibir. {@code kcal} é nulo se a energia de algum item for
 * desconhecida. {@code itemId} e {@code snapshot} só existem quando o lançamento tem um único item.
 */
public record EntryRow(String entryId, Occasion occasion, long createdAt, String title, int itemCount,
                       BigDecimal totalGrams, BigDecimal kcal, String itemId, Food snapshot) {

    public boolean isSingleItem() {
        return itemCount == 1;
    }
}
