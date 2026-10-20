package io.github.lucaslimacelino_dev.calju.data.local;

import android.content.res.AssetManager;

import androidx.annotation.WorkerThread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.domain.FoodMatcher;

/**
 * Copia o catálogo (CSVs gerados pelo catalog-builder e embutidos no APK) para o banco local.
 * Roda uma vez por versão do catálogo; aumente {@link #CATALOG_VERSION} ao regenerar os CSVs.
 */
public class CatalogImporter {

    public static final int CATALOG_VERSION = 1;

    private final CaljuDatabase database;
    private final AssetManager assets;

    public CatalogImporter(CaljuDatabase database, AssetManager assets) {
        this.database = database;
        this.assets = assets;
    }

    /** Retorna verdadeiro se importou; falso se o catálogo atual já estava no banco. */
    @WorkerThread
    public boolean importIfNeeded() throws IOException {
        CatalogDao dao = database.catalogDao();
        Integer imported = dao.catalogVersion();
        if (imported != null && imported == CATALOG_VERSION) {
            return false;
        }

        List<NutrientEntity> nutrients = new ArrayList<>();
        for (Map<String, String> row : read("nutrients.csv")) {
            nutrients.add(new NutrientEntity(row.get("code"), row.get("name_pt"), row.get("unit"),
                    Integer.parseInt(row.get("display_order"))));
        }

        List<FoodEntity> foods = new ArrayList<>();
        for (Map<String, String> row : read("foods.csv")) {
            String name = row.get("name");
            foods.add(new FoodEntity(row.get("id"), name, FoodMatcher.normalize(name), row.get("category"),
                    row.get("source_id"), Integer.valueOf(row.get("source_number"))));
        }

        List<FoodNutrientEntity> values = new ArrayList<>();
        for (Map<String, String> row : read("food_nutrients.csv")) {
            String amount = row.get("amount_per_100g");
            values.add(new FoodNutrientEntity(row.get("food_id"), row.get("nutrient_code"),
                    amount.isEmpty() ? null : new BigDecimal(amount), row.get("qualifier")));
        }

        List<FoodPortionEntity> portions = new ArrayList<>();
        for (Map<String, String> row : read("food_portions.csv")) {
            String foodId = row.get("food_id");
            String label = row.get("label");
            String id = UUID.nameUUIDFromBytes((foodId + ":" + label).getBytes(StandardCharsets.UTF_8)).toString();
            portions.add(new FoodPortionEntity(id, foodId, label, new BigDecimal(row.get("grams")),
                    "REFERENCE", row.get("source_id"), true));
        }

        database.runInTransaction(() -> {
            dao.insertNutrients(nutrients);
            dao.insertFoods(foods);
            dao.insertFoodNutrients(values);
            dao.insertPortions(portions);
            dao.saveCatalogInfo(new CatalogInfoEntity(CatalogInfoEntity.SINGLE_ROW_ID, CATALOG_VERSION));
        });
        return true;
    }

    private List<Map<String, String>> read(String fileName) throws IOException {
        List<Map<String, String>> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(assets.open(fileName), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IOException("Arquivo vazio: " + fileName);
            }
            List<String> header = CsvLine.parse(headerLine);
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isEmpty()) {
                    continue;
                }
                List<String> fields = CsvLine.parse(line);
                if (fields.size() != header.size()) {
                    throw new IOException(fileName + ", linha " + lineNumber + ": número de colunas inválido");
                }
                Map<String, String> row = new HashMap<>();
                for (int i = 0; i < header.size(); i++) {
                    row.put(header.get(i), fields.get(i));
                }
                rows.add(row);
            }
        }
        return rows;
    }
}
