package io.github.lucaslimacelino_dev.calju.catalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Uso: java -cp target/classes io.github.lucaslimacelino_dev.calju.catalog.Main
 * &lt;planilha-taco.xlsx&gt; &lt;pasta-de-saída&gt;
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.err.println("Uso: Main <planilha-taco.xlsx> <pasta-de-saída>");
            System.exit(2);
        }
        Path input = Path.of(args[0]);
        Path output = Path.of(args[1]);

        Map<Integer, Map<String, String>> rows = XlsxReader.readSheet(input, TacoParser.SHEET_NAME);
        List<CatalogFood> foods = TacoParser.parse(rows);

        Files.createDirectories(output);
        writeNutrients(output.resolve("nutrients.csv"));
        writeFoods(output.resolve("foods.csv"), foods);
        int values = writeFoodNutrients(output.resolve("food_nutrients.csv"), foods);

        System.out.printf("Alimentos: %d | Valores de nutrientes: %d | Saída: %s%n",
                foods.size(), values, output.toAbsolutePath());
    }

    private static void writeNutrients(Path file) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (Nutrient nutrient : TacoParser.nutrients()) {
            rows.add(List.of(nutrient.code(), nutrient.namePt(), nutrient.unit(),
                    String.valueOf(nutrient.displayOrder())));
        }
        CsvWriter.write(file, List.of("code", "name_pt", "unit", "display_order"), rows);
    }

    private static void writeFoods(Path file, List<CatalogFood> foods) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (CatalogFood food : foods) {
            rows.add(List.of(food.id().toString(), TacoParser.SOURCE_ID, String.valueOf(food.sourceNumber()),
                    food.name(), food.category()));
        }
        CsvWriter.write(file, List.of("id", "source_id", "source_number", "name", "category"), rows);
    }

    private static int writeFoodNutrients(Path file, List<CatalogFood> foods) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (CatalogFood food : foods) {
            for (NutrientValue value : food.nutrients()) {
                rows.add(List.of(food.id().toString(), value.nutrientCode(),
                        value.trace() ? "" : value.amount().toPlainString(),
                        value.trace() ? "TRACE" : "VALUE"));
            }
        }
        CsvWriter.write(file, List.of("food_id", "nutrient_code", "amount_per_100g", "qualifier"), rows);
        return rows.size();
    }
}
