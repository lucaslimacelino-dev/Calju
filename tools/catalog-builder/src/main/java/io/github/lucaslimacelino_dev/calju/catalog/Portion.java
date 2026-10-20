package io.github.lucaslimacelino_dev.calju.catalog;

import java.math.BigDecimal;
import java.util.UUID;

/** Porção padrão de um alimento do catálogo, com a fonte de onde o peso foi copiado. */
public record Portion(UUID foodId, String label, BigDecimal grams, String sourceId) {
}
