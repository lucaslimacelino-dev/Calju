package io.github.lucaslimacelino_dev.calju.data.repository;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Item a gravar (alimento + gramas). {@code itemId} identifica um item já gravado num registro
 * do diário (edição); é nulo para itens novos e para itens de refeição salva.
 */
public record ItemDraft(String itemId, String foodId, BigDecimal grams) {

    public ItemDraft {
        Objects.requireNonNull(grams, "grams");
        if (grams.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero: " + grams);
        }
    }

    public static ItemDraft of(String foodId, BigDecimal grams) {
        return new ItemDraft(null, Objects.requireNonNull(foodId, "foodId"), grams);
    }
}
