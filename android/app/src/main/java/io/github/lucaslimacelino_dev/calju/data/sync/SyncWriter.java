package io.github.lucaslimacelino_dev.calju.data.sync;

import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.FoodEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodNutrientEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodPortionEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogItemEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealItemEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.SyncStates;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.SyncDtos;
import io.github.lucaslimacelino_dev.calju.domain.FoodMatcher;

/**
 * Grava no aparelho a cópia que veio do servidor (trocando a local) e cria as cópias
 * "(conflito)" de alimentos e refeições. Quem chama abre a transação.
 */
final class SyncWriter {

    static final String CONFLICT_SUFFIX = " (conflito)";
    private static final int MAX_NAME = 200;

    private final CaljuDatabase database;
    private final Clock clock;

    SyncWriter(CaljuDatabase database, Clock clock) {
        this.database = database;
        this.clock = clock;
    }

    void food(SyncDtos.Food dto) {
        database.syncDao().deleteFood(dto.id());
        database.catalogDao().insertFood(SyncMapper.food(dto));
        database.catalogDao().insertFoodNutrients(SyncMapper.foodValues(dto));
        FoodPortionEntity portion = SyncMapper.foodPortion(dto);
        if (portion != null) {
            database.catalogDao().insertPortions(Collections.singletonList(portion));
        }
    }

    void meal(SyncDtos.Meal dto) {
        database.syncDao().deleteMeal(dto.id());
        database.mealDao().insertMeal(SyncMapper.meal(dto));
        database.mealDao().insertItems(SyncMapper.mealItems(dto));
    }

    void goal(SyncDtos.Goal dto) {
        database.syncDao().deleteGoal(dto.id());
        database.goalDao().insert(SyncMapper.goal(dto));
    }

    void entry(SyncDtos.LogEntry dto) {
        database.syncDao().deleteEntry(dto.id());
        database.diaryDao().insertEntry(SyncMapper.entry(dto));
        for (LogItemEntity item : SyncMapper.entryItems(dto)) {
            database.diaryDao().insertItem(item);
        }
        database.diaryDao().insertItemNutrients(SyncMapper.entryItemNutrients(dto));
    }

    /** Novo alimento, ainda não enviado, com o conteúdo do aparelho e o nome marcado. */
    String copyFood(FoodEntity food) {
        String id = UUID.randomUUID().toString();
        String name = conflictName(food.name);
        String searchName = FoodMatcher.normalize(food.brand == null ? name : name + " " + food.brand);
        FoodEntity copy = new FoodEntity(id, name, searchName, null, null, null, food.brand, true, clock.millis(),
                null, 1, SyncStates.PENDING);
        copy.unit = food.unit;
        database.catalogDao().insertFood(copy);
        List<FoodNutrientEntity> values = new ArrayList<>();
        for (FoodNutrientEntity value : database.catalogDao().nutrientsOf(food.id)) {
            values.add(new FoodNutrientEntity(id, value.nutrientCode, value.amountPer100g, value.qualifier));
        }
        database.catalogDao().insertFoodNutrients(values);
        FoodPortionEntity portion = database.catalogDao().defaultPortionOf(food.id);
        if (portion != null) {
            database.catalogDao().insertPortions(Collections.singletonList(new FoodPortionEntity(
                    UUID.randomUUID().toString(), id, portion.label, portion.grams, "USER", null, true)));
        }
        return id;
    }

    /** Nova refeição, ainda não enviada, com os itens do aparelho e o nome marcado. */
    String copyMeal(MealWithItems meal) {
        String id = UUID.randomUUID().toString();
        long now = clock.millis();
        database.mealDao().insertMeal(new MealEntity(id, conflictName(meal.meal.name), now, now, null, 1,
                SyncStates.PENDING));
        List<MealItemEntity> items = new ArrayList<>();
        for (MealItemEntity item : meal.items) {
            items.add(new MealItemEntity(UUID.randomUUID().toString(), id, item.foodId, item.quantityG,
                    item.position));
        }
        database.mealDao().insertItems(items);
        return id;
    }

    static String conflictName(String name) {
        String base = name.length() + CONFLICT_SUFFIX.length() > MAX_NAME
                ? name.substring(0, MAX_NAME - CONFLICT_SUFFIX.length()) : name;
        return base + CONFLICT_SUFFIX;
    }
}
