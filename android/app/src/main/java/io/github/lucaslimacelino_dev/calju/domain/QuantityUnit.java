package io.github.lucaslimacelino_dev.calju.domain;

/**
 * Unidade em que a quantidade de um alimento é digitada e mostrada. Os cálculos tratam 1 ml como
 * 1 g (aproximação aceita para bebidas; a tabela do rótulo é "por 100 ml").
 */
public enum QuantityUnit {
    GRAM("g"),
    MILLILITER("ml");

    private final String symbol;

    QuantityUnit(String symbol) {
        this.symbol = symbol;
    }

    public String symbol() {
        return symbol;
    }

    /** "ml" vira mililitro; qualquer outro valor, inclusive nulo, vira grama. */
    public static QuantityUnit fromSymbol(String symbol) {
        return MILLILITER.symbol.equals(symbol) ? MILLILITER : GRAM;
    }
}
