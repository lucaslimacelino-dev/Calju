package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;

import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/** Um lançamento do dia pronto para exibir. {@code kcal} é nulo se a energia for desconhecida. */
public record EntryRow(String entryId, String itemId, Occasion occasion, long createdAt, String foodName,
                       BigDecimal grams, BigDecimal kcal, Food snapshot) {
}
