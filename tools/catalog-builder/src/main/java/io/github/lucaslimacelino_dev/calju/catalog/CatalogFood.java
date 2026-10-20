package io.github.lucaslimacelino_dev.calju.catalog;

import java.util.List;
import java.util.UUID;

public record CatalogFood(UUID id, int sourceNumber, String name, String category, List<NutrientValue> nutrients) {
}
