package io.github.lucaslimacelino_dev.calju.data.repository;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogDao;
import io.github.lucaslimacelino_dev.calju.data.local.FoodEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodNutrientEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodPortionEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodSearchRow;
import io.github.lucaslimacelino_dev.calju.data.local.LogEntryEntity;
import io.github.lucaslimacelino_dev.calju.data.local.NutrientEntity;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.FoodMatcher;
import io.github.lucaslimacelino_dev.calju.domain.NutrientAmount;
import io.github.lucaslimacelino_dev.calju.domain.Portion;
import io.github.lucaslimacelino_dev.calju.domain.PortionSource;
import io.github.lucaslimacelino_dev.calju.domain.Qualifier;
import io.github.lucaslimacelino_dev.calju.domain.QuantityUnit;

/**
 * Catálogo de alimentos: os da TACO e os criados pelo usuário. Métodos acessam o banco: chamar
 * fora da thread principal. Alterar um alimento do usuário não muda registros já feitos (snapshot).
 */
public class CatalogRepository {

    private final CaljuDatabase database;
    private final CatalogDao dao;
    private final Clock clock;
    private volatile List<FoodSearchRow> searchIndex;

    public CatalogRepository(CaljuDatabase database, Clock clock) {
        this.database = database;
        this.dao = database.catalogDao();
        this.clock = clock;
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
        found.sort(Comparator.<FoodSearchRow, Boolean>comparing(row -> !row.featured())
                .thenComparingInt(row -> FoodMatcher.score(row.searchName, terms))
                .thenComparing(row -> row.searchName));
        return found.size() > limit ? new ArrayList<>(found.subList(0, limit)) : found;
    }

    @WorkerThread
    public List<NutrientEntity> nutrients() {
        return dao.allNutrients();
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
        String name = entity.displayName != null ? entity.displayName : entity.name;
        return new Food(name, nutrients, defaultPortion, QuantityUnit.fromSymbol(entity.unit));
    }

    public LiveData<List<FoodEntity>> observeCustomFoods() {
        return dao.observeCustomFoods();
    }

    @WorkerThread
    public String createCustomFood(CustomFoodData data) {
        String id = UUID.randomUUID().toString();
        String name = requireName(data.name());
        String brand = blankToNull(data.brand());
        database.runInTransaction(() -> {
            FoodEntity food = new FoodEntity(id, name, searchName(name, brand), null, null, null, brand, true,
                    clock.millis(), null, 1, LogEntryEntity.SYNC_PENDING);
            food.unit = data.unit().symbol();
            dao.insertFood(food);
            insertValues(id, data);
        });
        searchIndex = null;
        return id;
    }

    /** Substitui nome, marca, valores e porção. Moldes passam a usar os valores novos. */
    @WorkerThread
    public void updateCustomFood(String id, CustomFoodData data) {
        String name = requireName(data.name());
        String brand = blankToNull(data.brand());
        database.runInTransaction(() -> {
            dao.updateCustomFood(id, name, searchName(name, brand), brand, clock.millis());
            dao.setCustomFoodUnit(id, data.unit().symbol());
            dao.deleteNutrientsOf(id);
            dao.deletePortionsOf(id);
            insertValues(id, data);
        });
        searchIndex = null;
    }

    /** Lê um alimento do usuário para o formulário; nulo se não existir ou for do catálogo oficial. */
    @WorkerThread
    public CustomFoodData customFood(String id) {
        FoodEntity food = dao.food(id);
        if (food == null || !food.custom) {
            return null;
        }
        Map<String, BigDecimal> values = new HashMap<>();
        for (FoodNutrientEntity value : dao.nutrientsOf(id)) {
            if (value.amountPer100g != null) {
                values.put(value.nutrientCode, value.amountPer100g);
            }
        }
        FoodPortionEntity portion = dao.defaultPortionOf(id);
        return new CustomFoodData(food.name, food.brand, values,
                portion == null ? null : portion.label, portion == null ? null : portion.grams,
                QuantityUnit.fromSymbol(food.unit));
    }

    /** Alimentos mudaram por fora (sincronização): a busca recarrega a lista na próxima vez. */
    public void invalidateSearch() {
        searchIndex = null;
    }

    /** Exclusão lógica: some da busca e da lista, mas pode ser restaurado (desfazer). */
    @WorkerThread
    public void deleteCustomFood(String id) {
        long now = clock.millis();
        dao.setCustomFoodDeleted(id, now, now);
        searchIndex = null;
    }

    @WorkerThread
    public void restoreCustomFood(String id) {
        dao.setCustomFoodDeleted(id, null, clock.millis());
        searchIndex = null;
    }

    private void insertValues(String foodId, CustomFoodData data) {
        List<FoodNutrientEntity> values = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> value : data.per100g().entrySet()) {
            if (value.getValue().signum() < 0) {
                throw new IllegalArgumentException("Valor negativo: " + value.getKey());
            }
            values.add(new FoodNutrientEntity(foodId, value.getKey(), value.getValue(), "VALUE"));
        }
        dao.insertFoodNutrients(values);
        if (data.portionGrams() != null) {
            String label = blankToNull(data.portionLabel());
            if (data.portionGrams().signum() <= 0 || label == null) {
                throw new IllegalArgumentException("Porção precisa de nome e peso maior que zero");
            }
            dao.insertPortions(Collections.singletonList(new FoodPortionEntity(UUID.randomUUID().toString(),
                    foodId, label, data.portionGrams(), "USER", null, true)));
        }
    }

    private static String searchName(String name, String brand) {
        return FoodMatcher.normalize(brand == null ? name : name + " " + brand);
    }

    private static String requireName(String name) {
        String clean = blankToNull(name);
        if (clean == null) {
            throw new IllegalArgumentException("Nome do alimento é obrigatório");
        }
        return clean;
    }

    private static String blankToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value.trim();
    }

    /**
     * Os nomes são carregados uma vez; o catálogo tem poucas centenas de itens. Uma lista vazia
     * não é guardada, porque a importação do catálogo pode ainda não ter terminado. Criar,
     * alterar ou excluir um alimento do usuário descarta a lista, que é recarregada na próxima busca.
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
