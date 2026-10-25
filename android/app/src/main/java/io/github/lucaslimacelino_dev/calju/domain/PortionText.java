package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Quantidade em porções para mostrar ao usuário: "4 unidades médias", "1,5 xícara", "2 colheres de sopa". */
public final class PortionText {

    private static final BigDecimal TWO = new BigDecimal("2");
    private static final BigDecimal TOLERANCE = new BigDecimal("0.02");

    private PortionText() {
    }

    /**
     * Quantas porções há em {@code grams}, só quando dá um número "redondo" (múltiplo de meia
     * porção, como 1, 1,5 ou 4); caso contrário, nulo e a quantidade fica em gramas.
     */
    public static BigDecimal count(BigDecimal grams, BigDecimal portionGrams) {
        if (grams == null || portionGrams == null || portionGrams.signum() <= 0 || grams.signum() <= 0) {
            return null;
        }
        BigDecimal halves = grams.multiply(TWO).divide(portionGrams, 4, RoundingMode.HALF_UP);
        BigDecimal rounded = halves.setScale(0, RoundingMode.HALF_UP);
        if (rounded.signum() <= 0 || halves.subtract(rounded).abs().compareTo(TOLERANCE) > 0) {
            return null;
        }
        return normalize(rounded.divide(TWO, 1, RoundingMode.UNNECESSARY));
    }

    /** Porções exatas em {@code grams} (com até 2 casas), para converter o que foi digitado em gramas. */
    public static BigDecimal exactCount(BigDecimal grams, BigDecimal portionGrams) {
        return normalize(grams.divide(portionGrams, 2, RoundingMode.HALF_UP));
    }

    /** O rótulo sem o "1" inicial e no plural quando a quantidade passa de 1: "unidade média" → "unidades médias". */
    public static String noun(String label, BigDecimal count) {
        String noun = label.startsWith("1 ") ? label.substring(2) : label;
        if (count == null || count.compareTo(BigDecimal.ONE) <= 0) {
            return noun;
        }
        StringBuilder out = new StringBuilder();
        boolean pluralize = true;
        for (String word : noun.split(" ")) {
            if (word.equals("de") || word.equals("do") || word.equals("da") || word.equals("com")
                    || word.startsWith("(")) {
                pluralize = false;
            }
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(pluralize ? plural(word) : word);
        }
        return out.toString();
    }

    static String plural(String word) {
        if (word.isEmpty() || !Character.isLetter(word.charAt(word.length() - 1))) {
            return word;
        }
        char last = Character.toLowerCase(word.charAt(word.length() - 1));
        if ("aeiouáéíóúâêô".indexOf(last) >= 0) {
            return word + "s";
        }
        if (last == 'r' || last == 'z') {
            return word + "es";
        }
        if (last == 'm') {
            return word.substring(0, word.length() - 1) + "ns";
        }
        if (last == 'l') {
            return word.substring(0, word.length() - 1) + "is";
        }
        return word;
    }

    private static BigDecimal normalize(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
    }
}
