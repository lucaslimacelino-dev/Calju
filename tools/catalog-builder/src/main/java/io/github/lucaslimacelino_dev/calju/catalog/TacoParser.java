package io.github.lucaslimacelino_dev.calju.catalog;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Converte a aba de composição da TACO 4ª edição em alimentos do catálogo.
 * Regras: "Tr" é traço; vazio, "NA" e "*" são desconhecidos (o nutriente não entra);
 * carboidrato negativo vira zero; qualquer outro texto não numérico ou valor negativo
 * interrompe a geração.
 */
public final class TacoParser {

    public static final String SOURCE_ID = "TACO-4";
    public static final String SHEET_NAME = "CMVCol taco3";

    private static final int SCALE = 4;

    /**
     * Células com texto inválido na planilha oficial, tratadas como desconhecidas
     * (ver docs/fontes-de-dados.md). Formato: número do alimento na TACO + ":" + coluna.
     */
    private static final Set<String> KNOWN_INVALID_CELLS = Set.of("373:AA");

    /** Coluna da planilha, rótulo e unidade esperados no cabeçalho e nutriente correspondente. */
    private record Column(String letter, String header, String unitHeader, Nutrient nutrient) {
    }

    private static final List<Column> COLUMNS = List.of(
            new Column("D", "Energia", "(kcal)", new Nutrient("ENERC", "Energia", "kcal", 1)),
            new Column("F", "Proteína", "(g)", new Nutrient("PROCNT", "Proteína", "g", 2)),
            new Column("I", "idrato", "(g)", new Nutrient("CHOCDF", "Carboidrato", "g", 3)),
            new Column("G", "Lipídeos", "(g)", new Nutrient("FAT", "Gorduras totais", "g", 4)),
            new Column("J", "Alimentar", "(g)", new Nutrient("FIBTG", "Fibra alimentar", "g", 5)),
            new Column("H", "Colesterol", "(mg)", new Nutrient("CHOLE", "Colesterol", "mg", 6)),
            new Column("R", "Sódio", "(mg)", new Nutrient("NA", "Sódio", "mg", 7)),
            new Column("S", "Potássio", "(mg)", new Nutrient("K", "Potássio", "mg", 8)),
            new Column("L", "Cálcio", "(mg)", new Nutrient("CA", "Cálcio", "mg", 9)),
            new Column("Q", "Ferro", "(mg)", new Nutrient("FE", "Ferro", "mg", 10)),
            new Column("M", "Magnésio", "(mg)", new Nutrient("MG", "Magnésio", "mg", 11)),
            new Column("P", "Fósforo", "(mg)", new Nutrient("P", "Fósforo", "mg", 12)),
            new Column("U", "Zinco", "(mg)", new Nutrient("ZN", "Zinco", "mg", 13)),
            new Column("T", "Cobre", "(mg)", new Nutrient("CU", "Cobre", "mg", 14)),
            new Column("O", "Manganês", "(mg)", new Nutrient("MN", "Manganês", "mg", 15)),
            new Column("AC", "C", "(mg)", new Nutrient("VITC", "Vitamina C", "mg", 16)),
            new Column("W", "RE", "(mcg)", new Nutrient("VITA", "Vitamina A (RE)", "µg", 17)),
            new Column("V", "Retinol", "(mcg)", new Nutrient("RETOL", "Retinol", "µg", 18)),
            new Column("Y", "Tiamina", "(mg)", new Nutrient("THIA", "Tiamina (B1)", "mg", 19)),
            new Column("Z", "Riboflavina", "(mg)", new Nutrient("RIBF", "Riboflavina (B2)", "mg", 20)),
            new Column("AA", "Piridoxina", "(mg)", new Nutrient("VITB6A", "Piridoxina (B6)", "mg", 21)),
            new Column("AB", "Niacina", "(mg)", new Nutrient("NIA", "Niacina", "mg", 22)),
            new Column("C", "Umidade", "(%)", new Nutrient("WATER", "Umidade", "g", 23)),
            new Column("K", "Cinzas", "(g)", new Nutrient("ASH", "Cinzas", "g", 24)));

    private TacoParser() {
    }

    public static List<Nutrient> nutrients() {
        return COLUMNS.stream().map(Column::nutrient).toList();
    }

    public static List<CatalogFood> parse(Map<Integer, Map<String, String>> rows) {
        validateHeader(rows);

        List<CatalogFood> foods = new ArrayList<>();
        String category = null;
        for (Map.Entry<Integer, Map<String, String>> entry : rows.entrySet()) {
            Map<String, String> row = entry.getValue();
            String first = text(row, "A");
            String description = text(row, "B");

            if (first.equals("Legenda")) {
                break;
            }
            if (isInteger(first) && !description.isEmpty()) {
                if (category == null) {
                    throw new IllegalStateException("Alimento sem categoria na linha " + entry.getKey());
                }
                int number = Integer.parseInt(first);
                List<NutrientValue> values = values(row, entry.getKey(), number);
                if (!values.isEmpty()) {
                    foods.add(new CatalogFood(idFor(number), number, normalizeName(description), category, values));
                }
            } else if (!first.isEmpty() && description.isEmpty() && !isHeaderRow(row)) {
                category = first;
            }
        }
        return foods;
    }

    /** Identificador estável: o mesmo número da TACO gera sempre o mesmo UUID. */
    public static UUID idFor(int sourceNumber) {
        return UUID.nameUUIDFromBytes((SOURCE_ID + ":" + sourceNumber).getBytes(StandardCharsets.UTF_8));
    }

    private static List<NutrientValue> values(Map<String, String> row, int rowNumber, int foodNumber) {
        List<NutrientValue> values = new ArrayList<>();
        for (Column column : COLUMNS) {
            String raw = text(row, column.letter());
            String code = column.nutrient().code();
            if (raw.isEmpty() || raw.equals("NA") || raw.equals("*")
                    || KNOWN_INVALID_CELLS.contains(foodNumber + ":" + column.letter())) {
                continue;
            }
            if (raw.equals("Tr")) {
                values.add(new NutrientValue(code, null, true));
                continue;
            }
            BigDecimal amount;
            try {
                amount = new BigDecimal(raw).setScale(SCALE, RoundingMode.HALF_UP);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("Valor não reconhecido \"" + raw + "\" na célula "
                        + column.letter() + rowNumber, e);
            }
            if (amount.signum() < 0) {
                // Carboidrato é calculado por diferença; a TACO impressa mostra 0,0 nesses casos.
                if (!code.equals("CHOCDF")) {
                    throw new IllegalStateException("Valor negativo na célula " + column.letter() + rowNumber);
                }
                amount = BigDecimal.ZERO.setScale(SCALE);
            }
            values.add(new NutrientValue(code, amount, false));
        }
        return values;
    }

    /** Garante que cada coluna e unidade estão onde esperamos, para não trocar nutrientes em silêncio. */
    private static void validateHeader(Map<Integer, Map<String, String>> rows) {
        Map<String, String> names = firstRowWhere(rows, "C", "Umidade");
        Map<String, String> units = firstRowWhere(rows, "B", "Descrição dos alimentos");
        for (Column column : COLUMNS) {
            expect(names, column.letter(), column.header());
            expect(units, column.letter(), column.unitHeader());
        }
    }

    private static Map<String, String> firstRowWhere(Map<Integer, Map<String, String>> rows, String column,
                                                     String value) {
        return rows.values().stream()
                .filter(row -> text(row, column).equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Cabeçalho da TACO não encontrado: " + value));
    }

    private static void expect(Map<String, String> row, String column, String expected) {
        String found = text(row, column);
        if (!found.equals(expected)) {
            throw new IllegalStateException("Coluna " + column + ": esperado \"" + expected
                    + "\", encontrado \"" + found + "\"");
        }
    }

    private static boolean isHeaderRow(Map<String, String> row) {
        String first = text(row, "A");
        return first.startsWith("Número do") || first.equals("Alimento") || text(row, "C").equals("Umidade");
    }

    private static boolean isInteger(String value) {
        return !value.isEmpty() && value.chars().allMatch(Character::isDigit);
    }

    private static String normalizeName(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private static String text(Map<String, String> row, String column) {
        String value = row.get(column);
        return value == null ? "" : value.trim();
    }
}
