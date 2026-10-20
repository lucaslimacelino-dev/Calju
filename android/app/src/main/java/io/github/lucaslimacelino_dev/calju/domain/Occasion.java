package io.github.lucaslimacelino_dev.calju.domain;

import java.time.LocalTime;
import java.util.Objects;

/** Ocasião do registro, na ordem em que aparece no diário. */
public enum Occasion {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK;

    /** Sugestão pelo horário: café 5h–10h59, almoço 11h–14h59, jantar 18h–21h59, lanche no resto. */
    public static Occasion suggestFor(LocalTime time) {
        Objects.requireNonNull(time, "time");
        int hour = time.getHour();
        if (hour >= 5 && hour < 11) {
            return BREAKFAST;
        }
        if (hour >= 11 && hour < 15) {
            return LUNCH;
        }
        if (hour >= 18 && hour < 22) {
            return DINNER;
        }
        return SNACK;
    }
}
