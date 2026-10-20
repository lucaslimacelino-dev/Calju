package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.util.Objects;

/** Medida caseira com seu peso em gramas, por exemplo "1 unidade média". */
public record Portion(String label, BigDecimal grams, PortionSource source) {

    public Portion {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(grams, "grams");
        Objects.requireNonNull(source, "source");
        if (grams.signum() <= 0) {
            throw new IllegalArgumentException("Peso da porção deve ser maior que zero: " + grams);
        }
    }
}
