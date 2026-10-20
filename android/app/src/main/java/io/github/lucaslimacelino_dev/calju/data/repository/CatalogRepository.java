package io.github.lucaslimacelino_dev.calju.data.repository;

import androidx.annotation.WorkerThread;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.lucaslimacelino_dev.calju.data.local.CatalogDao;
import io.github.lucaslimacelino_dev.calju.data.local.FoodEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodNutrientEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodPortionEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodSearchRow;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.FoodMatcher;
import io.github.lucaslimacelino_dev.calju.domain.NutrientAmount;
import io.github.lucaslimacelino_dev.calju.domain.Portion;
import io.github.lucaslimacelino_dev.calju.domain.PortionSource;
import io.github.lucaslimacelino_dev.calju.domain.Qualifier;

/** Busca e leitura do catálogo de alimentos. Métodos acessam o banco: chamar fora da thread principal. */
public class CatalogRepository {

    private final CatalogDao dao;
    private volatile List<FoodSearchRow> searchIndex;

    public CatalogRepository(CatalogDao dao) {
        this.dao = dao;
    }

    @WorkerThread
    public List<FoodSearchRow> search(String query, int limit) {
        List<String> terms = FoodMatcher.terms(query);
        if (terms.isEmpty()) {
            return new ArrayList<>();
        }
        List<FoodSearchRow> found = new ArrayList<>();
        for (FoodSearchRow row : index()) {
            if (FoodMatcher.matches(row.searchName, terms)) {
                found.add(row);
            }
        }
        found.sort(Comparator.<FoodSearchRow>comparingInt(row -> FoodMatcher.score(row.searchName, terms))
                .thenComparing(row -> row.searchName));
        return found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
    }

    @WorkerThread
    public Food food(String id) {
        FoodEntity entity = dao.food(id);
        if (entity == null) {
            return null;
        }
        Map<String, NutrientAmount> nutrients = new HashMap<>();
        for (FoodNutrientEntity value : dao.nutrientsOf(id)) {
            NutrientAmount amount = "TRACE".equals(value.qualifier)
                    ? NutrientAmount.trace()
                    : new NutrientAmount(value.amountPer100g, Qualifier.VALUE);
            nutrients.put(value.nutrientCode, amount);
        }
        FoodPortionEntity portion = dao.defaultPortionOf(id);
        Portion defaultPortion = portion == null ? null
                : new Portion(portion.label, portion.grams,
                "USER".equals(portion.source) ? PortionSource.USER : PortionSource.REFERENCE);
        return new Food(entity.name, nutrients, defaultPortion);
    }

    /**
     * Os nomes são carregados uma vez; o catálogo tem poucas centenas de itens. Uma lista vazia
     * não é guardada, porque a importação do catálogo pode ainda não ter terminado.
     */
    private List<FoodSearchRow> index() {
        List<FoodSearchRow> current = searchIndex;
        if (current == null) {
            current = dao.allForSearch();
            if (!current.isEmpty()) {
                searchIndex = current;
            }
        }
        return current;
    }
}
