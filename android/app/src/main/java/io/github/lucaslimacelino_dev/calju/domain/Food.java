package io.github.lucaslimacelino_dev.calju.domain;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Alimento com os nutrientes por 100 g (ou 100 ml, ver {@link QuantityUnit}), indexados pelo
 * código INFOODS. Nutriente ausente do mapa significa valor desconhecido, nunca zero.
 */
public record Food(String name, Map<String, NutrientAmount> nutrientsPer100g, Portion defaultPortion,
                   QuantityUnit unit) {

    public Food {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(nutrientsPer100g, "nutrientsPer100g");
        nutrientsPer100g = Collections.unmodifiableMap(new HashMap<>(nutrientsPer100g));
        unit = unit == null ? QuantityUnit.GRAM : unit;
    }

    /** Alimento medido em gramas. */
    public Food(String name, Map<String, NutrientAmount> nutrientsPer100g, Portion defaultPortion) {
        this(name, nutrientsPer100g, defaultPortion, QuantityUnit.GRAM);
    }

    public Optional<NutrientAmount> nutrient(String code) {
        return Optional.ofNullable(nutrientsPer100g.get(code));
    }

    public Optional<Portion> portion() {
        return Optional.ofNullable(defaultPortion);
    }
}
