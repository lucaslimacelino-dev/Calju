package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.room.TypeConverter;

import java.math.BigDecimal;

/** O SQLite não tem tipo decimal exato; BigDecimal é guardado como TEXT para não perder precisão. */
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
}
