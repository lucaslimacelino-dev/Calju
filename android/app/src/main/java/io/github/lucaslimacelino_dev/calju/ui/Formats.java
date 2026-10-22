package io.github.lucaslimacelino_dev.calju.ui;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;

/** Números no formato brasileiro (1.240; 37,5). O arredondamento acontece só aqui, na exibição. */
public final class Formats {

    private static final DecimalFormatSymbols PT_BR = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR"));

    private Formats() {
    }

    /** Gramas com até 1 casa decimal: 150 → "150 g", 37,5 → "37,5 g". */
    public static String grams(BigDecimal grams) {
        return pattern("#,##0.#").format(NutrientUnit.GRAM.roundForDisplay(grams)) + " g";
    }

    /** Quantidade de unidades: 2 → "2", 1,5 → "1,5". */
    public static String count(BigDecimal count) {
        return pattern("#,##0.##").format(count);
    }

    /** Valor de nutriente arredondado conforme a unidade, sem o símbolo. */
    public static String amount(BigDecimal value, NutrientUnit unit) {
        BigDecimal rounded = unit.roundForDisplay(value);
        return pattern(rounded.scale() > 0 ? "#,##0.0" : "#,##0").format(rounded);
    }

    /** Converte o texto digitado ("37,5" ou "37.5") em número; nulo se inválido ou não positivo. */
    public static BigDecimal parsePositive(String text) {
        BigDecimal value = parse(text);
        return value != null && value.signum() > 0 ? value : null;
    }

    /** Como {@link #parsePositive}, mas aceita zero (valores de rótulo podem ser 0). */
    public static BigDecimal parseNonNegative(String text) {
        BigDecimal value = parse(text);
        return value != null && value.signum() >= 0 ? value : null;
    }

    private static BigDecimal parse(String text) {
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static DecimalFormat pattern(String pattern) {
        return new DecimalFormat(pattern, PT_BR);
    }
}
