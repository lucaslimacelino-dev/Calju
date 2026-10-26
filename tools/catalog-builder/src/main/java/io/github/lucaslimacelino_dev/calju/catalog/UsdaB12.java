package io.github.lucaslimacelino_dev.calju.catalog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Vitamina B12, que a TACO não traz, para os alimentos comuns (data/fontes/usda/b12-alimentos-comuns.csv):
 * alimentos de origem animal com o valor do alimento mais parecido do USDA FoodData Central (SR Legacy,
 * domínio público) e alimentos só de origem vegetal com 0. Alimentos fora da lista ficam sem valor.
 */
public final class UsdaB12 {

    public static final Nutrient NUTRIENT = new Nutrient("VITB12", "Vitamina B12", "µg", 26);

    private UsdaB12() {
    }

    public static List<CatalogFood> addTo(List<CatalogFood> foods, List<Map<String, String>> curated) {
        Map<Integer, BigDecimal> b12 = new HashMap<>();
        Map<Integer, CatalogFood> byNumber = new HashMap<>();
        for (CatalogFood food : foods) {
            byNumber.put(food.sourceNumber(), food);
        }
        for (Map<String, String> row : curated) {
            int number = Integer.parseInt(row.get("taco_number"));
            CatalogFood food = byNumber.get(number);
            if (food == null) {
                throw new IllegalStateException("Alimento " + number + " da TACO não está no catálogo");
            }
            if (!food.name().equals(row.get("taco_name"))) {
                throw new IllegalStateException("Alimento " + number + ": nome na curadoria \"" + row.get("taco_name")
                        + "\" difere do catálogo \"" + food.name() + "\"");
            }
            BigDecimal amount = new BigDecimal(row.get("b12_ug_per_100g"));
            if (amount.signum() < 0) {
                throw new IllegalStateException("Alimento " + number + ": B12 não pode ser negativa");
            }
            if (b12.put(number, amount) != null) {
                throw new IllegalStateException("Alimento " + number + " repetido na curadoria de B12");
            }
        }

        List<CatalogFood> result = new ArrayList<>();
        for (CatalogFood food : foods) {
            BigDecimal amount = b12.get(food.sourceNumber());
            if (amount == null) {
                result.add(food);
                continue;
            }
            List<NutrientValue> values = new ArrayList<>(food.nutrients());
            values.add(new NutrientValue(NUTRIENT.code(), amount.setScale(TacoParser.SCALE), false));
            result.add(new CatalogFood(food.id(), food.sourceNumber(), food.name(), food.category(), values));
        }
        return result;
    }
}
