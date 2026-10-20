package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta o texto do campo de registro.
 * <ul>
 *   <li>"arroz 150", "arroz 150g", "150 g de arroz": 150 gramas.</li>
 *   <li>"2 bananas", "1,5 banana": unidades da porção padrão.</li>
 *   <li>"banana": sem quantidade.</li>
 * </ul>
 */
public final class FoodInputParser {

    private static final String NUMBER = "(\\d+(?:[.,]\\d+)?)";
    private static final String GRAM_UNIT = "(?:g|gr|grama|gramas)";

    private static final Pattern LEADING_GRAMS = Pattern.compile(
            "^" + NUMBER + "\\s*" + GRAM_UNIT + "\\s+(?:de\\s+)?(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern LEADING_UNITS = Pattern.compile(
            "^" + NUMBER + "\\s+(?:de\\s+)?(.+)$", Pattern.CASE_INSENSITIVE);
    private static final Pattern TRAILING_GRAMS = Pattern.compile(
            "^(.+?)\\s+" + NUMBER + "\\s*(?:" + GRAM_UNIT + ")?$", Pattern.CASE_INSENSITIVE);

    private FoodInputParser() {
    }

    public static FoodInput parse(String text) {
        Objects.requireNonNull(text, "text");
        String rest = text.trim().replaceAll("\\s+", " ");
        BigDecimal grams = null;
        BigDecimal units = null;

        Matcher leadingGrams = LEADING_GRAMS.matcher(rest);
        Matcher leadingUnits = LEADING_UNITS.matcher(rest);
        if (leadingGrams.matches()) {
            grams = number(leadingGrams.group(1));
            rest = leadingGrams.group(2);
        } else if (leadingUnits.matches()) {
            units = number(leadingUnits.group(1));
            rest = leadingUnits.group(2);
        }

        if (grams == null) {
            Matcher trailingGrams = TRAILING_GRAMS.matcher(rest);
            if (trailingGrams.matches()) {
                grams = number(trailingGrams.group(2));
                rest = trailingGrams.group(1);
            }
        }

        return new FoodInput(rest.trim(), positiveOrNull(grams), positiveOrNull(units));
    }

    private static BigDecimal number(String digits) {
        return new BigDecimal(digits.replace(',', '.'));
    }

    private static BigDecimal positiveOrNull(BigDecimal value) {
        return value != null && value.signum() > 0 ? value : null;
    }
}
