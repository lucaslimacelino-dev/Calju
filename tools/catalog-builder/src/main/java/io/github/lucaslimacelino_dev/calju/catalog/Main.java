package io.github.lucaslimacelino_dev.calju.catalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Uso: java -cp target/classes io.github.lucaslimacelino_dev.calju.catalog.Main
 * &lt;planilha-taco.xlsx&gt; &lt;porcoes-ibge.csv&gt; &lt;pasta-de-saída&gt;
 */
public final class Main {

    private Main() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            System.err.println("Uso: Main <planilha-taco.xlsx> <porcoes-ibge.csv> <pasta-de-saída>");
            System.exit(2);
        }
        Path tacoFile = Path.of(args[0]);
        Path portionsFile = Path.of(args[1]);
        Path output = Path.of(args[2]);

        Map<Integer, Map<String, String>> rows = XlsxReader.readSheet(tacoFile, TacoParser.SHEET_NAME);
        List<CatalogFood> foods = TacoParser.parse(rows);
        List<Portion> portions = IbgePortions.link(CsvReader.read(portionsFile), foods);

        Files.createDirectories(output);
        writeNutrients(output.resolve("nutrients.csv"));
        writeFoods(output.resolve("foods.csv"), foods);
        int values = writeFoodNutrients(output.resolve("food_nutrients.csv"), foods);
        writePortions(output.resolve("food_portions.csv"), portions);

        System.out.printf("Alimentos: %d | Valores de nutrientes: %d | Porções: %d | Saída: %s%n",
                foods.size(), values, portions.size(), output.toAbsolutePath());
    }

    private static void writePortions(Path file, List<Portion> portions) throws IOException {
        List<List<String>> rows = new ArrayList<>();
        for (Portion portion : portions) {
            rows.add(List.of(portion.foodId().toString(), portion.label(), portion.grams().toPlainString(),
                    portion.sourceId()));
        }
        CsvWriter.write(file, List.of("food_id", "label", "grams", "source_id"), rows);
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
