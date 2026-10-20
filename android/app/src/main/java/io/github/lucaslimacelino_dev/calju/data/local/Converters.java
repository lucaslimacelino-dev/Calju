package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.room.TypeConverter;

import java.math.BigDecimal;
import java.time.LocalDate;

/** O SQLite não tem tipo decimal exato nem data; ambos são guardados como TEXT. */
public final class Converters {

    private Converters() {
    }

    @TypeConverter
    public static String fromBigDecimal(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }

    @TypeConverter
    public static BigDecimal toBigDecimal(String value) {
        return value == null ? null : new BigDecimal(value);
    }

    /** Datas do diário como texto ISO (2026-10-20): ordenam corretamente e são legíveis. */
    @TypeConverter
    public static String fromLocalDate(LocalDate date) {
        return date == null ? null : date.toString();
    }

    @TypeConverter
    public static LocalDate toLocalDate(String value) {
        return value == null ? null : LocalDate.parse(value);
    }
}
