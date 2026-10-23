package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;

import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import io.github.lucaslimacelino_dev.calju.domain.QuantityUnit;

/**
 * Um lançamento do dia pronto para exibir. {@code kcal} é nulo se a energia de algum item for
 * desconhecida. {@code itemId} e {@code snapshot} só existem quando o lançamento tem um único item.
 * {@code unit} é ml só quando todos os itens são em ml (1 ml conta como 1 g no total).
 */
public record EntryRow(String entryId, Occasion occasion, long createdAt, String title, int itemCount,
                       BigDecimal totalGrams, QuantityUnit unit, BigDecimal kcal, String itemId, Food snapshot) {

    public boolean isSingleItem() {
        return itemCount == 1;
    }
}
