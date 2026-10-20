package io.github.lucaslimacelino_dev.calju.catalog;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Liga as porções curadas da tabela de medidas do IBGE (data/fontes/ibge/porcoes-unidade.csv)
 * aos alimentos do catálogo, conferindo número e nome de cada alimento na TACO.
 */
public final class IbgePortions {

    public static final String SOURCE_ID = "IBGE-POF-2008-2009-MEDIDAS";

    private IbgePortions() {
    }

    public static List<Portion> link(List<Map<String, String>> curated, List<CatalogFood> foods) {
        Map<Integer, CatalogFood> byNumber = new HashMap<>();
        for (CatalogFood food : foods) {
            byNumber.put(food.sourceNumber(), food);
        }

        List<Portion> portions = new ArrayList<>();
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
            BigDecimal grams = new BigDecimal(row.get("grams"));
            if (grams.signum() <= 0) {
                throw new IllegalStateException("Alimento " + number + ": peso da porção deve ser positivo");
            }
            portions.add(new Portion(food.id(), row.get("label"), grams, SOURCE_ID));
        }
        return portions;
    }
}
