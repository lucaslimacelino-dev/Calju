package io.github.lucaslimacelino_dev.calju.ui;

import android.content.Context;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import io.github.lucaslimacelino_dev.calju.R;

/** Nomes de dias em português: "Hoje", "Ontem", "Terça-feira", "terça-feira, 20 de outubro". */
public final class Dates {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter WEEKDAY = DateTimeFormatter.ofPattern("EEEE", PT_BR);
    private static final DateTimeFormatter FULL = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", PT_BR);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("EEE, d 'de' MMM", PT_BR);

    private Dates() {
    }

    /** "Hoje", "Ontem" ou o dia da semana. */
    public static String title(Context context, LocalDate date, LocalDate today) {
        if (date.equals(today)) {
            return context.getString(R.string.day_today);
        }
        if (date.equals(today.minusDays(1))) {
            return context.getString(R.string.day_yesterday);
        }
        return capitalize(WEEKDAY.format(date));
    }

    /** "Hoje", "Ontem" ou a data curta ("Ter., 20 de out."), para listas. */
    public static String shortTitle(Context context, LocalDate date, LocalDate today) {
        if (date.equals(today) || date.equals(today.minusDays(1))) {
            return title(context, date, today);
        }
        return capitalize(SHORT.format(date));
    }

    public static String full(LocalDate date) {
        return FULL.format(date);
    }

    private static String capitalize(String text) {
        return text.isEmpty() ? text : text.substring(0, 1).toUpperCase(PT_BR) + text.substring(1);
    }
}
