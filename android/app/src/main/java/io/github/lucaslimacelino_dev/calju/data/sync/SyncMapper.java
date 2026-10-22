package io.github.lucaslimacelino_dev.calju.data.sync;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.FoodEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodNutrientEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodPortionEntity;
import io.github.lucaslimacelino_dev.calju.data.local.ItemWithNutrients;
import io.github.lucaslimacelino_dev.calju.data.local.LogEntryEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogItemEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogItemNutrientEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealItemEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.NutritionGoalEntity;
import io.github.lucaslimacelino_dev.calju.data.local.SyncStates;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.SyncDtos;
import io.github.lucaslimacelino_dev.calju.domain.FoodMatcher;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/**
 * Converte entre as tabelas do aparelho e o formato da API. Ida: o corpo enviado leva a versão do
 * servidor que o aparelho conhece. Volta: o registro chega já marcado como sincronizado.
 */
final class SyncMapper {

    private SyncMapper() {
    }

    // Aparelho → servidor

    static SyncDtos.GoalBody goalBody(NutritionGoalEntity goal) {
        return new SyncDtos.GoalBody(goal.effectiveFrom.toString(), goal.kcal, goal.proteinG, goal.carbsG,
                goal.fatG, goal.serverVersion);
    }

    static SyncDtos.FoodBody foodBody(FoodEntity food, List<FoodNutrientEntity> values, FoodPortionEntity portion) {
        return new SyncDtos.FoodBody(food.name, food.brand, nutrients(values),
                portion == null ? null : new SyncDtos.Portion(portion.label, portion.grams), food.serverVersion);
    }

    static SyncDtos.MealBody mealBody(MealWithItems meal) {
        List<MealItemEntity> sorted = new ArrayList<>(meal.items);
        sorted.sort(Comparator.comparingInt(item -> item.position));
        List<SyncDtos.MealItem> items = new ArrayList<>();
        for (MealItemEntity item : sorted) {
            items.add(new SyncDtos.MealItem(item.id, item.foodId, item.quantityG));
        }
        return new SyncDtos.MealBody(meal.meal.name, items, meal.meal.serverVersion);
    }

    static SyncDtos.LogEntryBody entryBody(EntryWithItems entry) {
        List<ItemWithNutrients> sorted = new ArrayList<>(entry.items);
        sorted.sort(Comparator.comparingInt(item -> item.item.position));
        List<SyncDtos.LogItem> items = new ArrayList<>();
        for (ItemWithNutrients item : sorted) {
            List<SyncDtos.NutrientValue> values = new ArrayList<>();
            for (LogItemNutrientEntity value : item.nutrients) {
                values.add(new SyncDtos.NutrientValue(value.nutrientCode, value.amountPer100g, value.qualifier));
            }
            LogItemEntity it = item.item;
            items.add(new SyncDtos.LogItem(it.id, it.foodId, it.foodNameSnap, it.brandSnap, it.sourceSnap,
                    it.quantityG, it.portionLabelSnap, values));
        }
        LogEntryEntity e = entry.entry;
        return new SyncDtos.LogEntryBody(e.logDate.toString(), e.occasion.name(), e.title, e.sourceMealId,
                Instant.ofEpochMilli(e.createdAt).toString(), items, e.serverVersion);
    }

    private static List<SyncDtos.NutrientValue> nutrients(List<FoodNutrientEntity> values) {
        List<SyncDtos.NutrientValue> result = new ArrayList<>();
        for (FoodNutrientEntity value : values) {
            result.add(new SyncDtos.NutrientValue(value.nutrientCode, value.amountPer100g, value.qualifier));
        }
        return result;
    }

    // Servidor → aparelho

    static NutritionGoalEntity goal(SyncDtos.Goal dto) {
        NutritionGoalEntity goal = new NutritionGoalEntity(dto.id(), LocalDate.parse(dto.effectiveFrom()), dto.kcal(),
                dto.proteinG(), dto.carbohydrateG(), dto.fatG(), millis(dto.updatedAt()), dto.version(),
                SyncStates.SYNCED);
        goal.serverVersion = dto.version();
        return goal;
    }

    static FoodEntity food(SyncDtos.Food dto) {
        String searchName = FoodMatcher.normalize(dto.brand() == null ? dto.name() : dto.name() + " " + dto.brand());
        FoodEntity food = new FoodEntity(dto.id(), dto.name(), searchName, null, null, null, dto.brand(), true,
                millis(dto.updatedAt()), dto.deletedAt() == null ? null : millis(dto.deletedAt()), dto.version(),
                SyncStates.SYNCED);
        food.serverVersion = dto.version();
        return food;
    }

    static List<FoodNutrientEntity> foodValues(SyncDtos.Food dto) {
        List<FoodNutrientEntity> values = new ArrayList<>();
        for (SyncDtos.NutrientValue value : dto.nutrients()) {
            values.add(new FoodNutrientEntity(dto.id(), value.code(), value.amount(), value.qualifier()));
        }
        return values;
    }

    /** Porção com id derivado do alimento: aplicar a mesma versão duas vezes gera a mesma linha. */
    static FoodPortionEntity foodPortion(SyncDtos.Food dto) {
        if (dto.portion() == null) {
            return null;
        }
        String id = UUID.nameUUIDFromBytes(("portion:" + dto.id()).getBytes(StandardCharsets.UTF_8)).toString();
        return new FoodPortionEntity(id, dto.id(), dto.portion().label(), dto.portion().grams(), "USER", null, true);
    }

    static MealEntity meal(SyncDtos.Meal dto) {
        long updated = millis(dto.updatedAt());
        MealEntity meal = new MealEntity(dto.id(), dto.name(), updated, updated,
                dto.deletedAt() == null ? null : millis(dto.deletedAt()), dto.version(), SyncStates.SYNCED);
        meal.serverVersion = dto.version();
        return meal;
    }

    static List<MealItemEntity> mealItems(SyncDtos.Meal dto) {
        List<MealItemEntity> items = new ArrayList<>();
        for (int i = 0; i < dto.items().size(); i++) {
            SyncDtos.MealItem item = dto.items().get(i);
            items.add(new MealItemEntity(item.id(), dto.id(), item.foodId(), item.quantityG(), i));
        }
        return items;
    }

    static LogEntryEntity entry(SyncDtos.LogEntry dto) {
        LogEntryEntity entry = new LogEntryEntity(dto.id(), LocalDate.parse(dto.logDate()),
                Occasion.valueOf(dto.occasion()), dto.title(), dto.sourceMealId(), millis(dto.createdAt()),
                millis(dto.updatedAt()), dto.deletedAt() == null ? null : millis(dto.deletedAt()), dto.version(),
                SyncStates.SYNCED);
        entry.serverVersion = dto.version();
        return entry;
    }

    static List<LogItemEntity> entryItems(SyncDtos.LogEntry dto) {
        List<LogItemEntity> items = new ArrayList<>();
        for (int i = 0; i < dto.items().size(); i++) {
            SyncDtos.LogItem item = dto.items().get(i);
            items.add(new LogItemEntity(item.id(), dto.id(), item.foodId(), item.foodName(), item.brand(),
                    item.source(), item.quantityG(), item.portionLabel(), i));
        }
        return items;
    }

    static List<LogItemNutrientEntity> entryItemNutrients(SyncDtos.LogEntry dto) {
        List<LogItemNutrientEntity> values = new ArrayList<>();
        for (SyncDtos.LogItem item : dto.items()) {
            for (SyncDtos.NutrientValue value : item.nutrients()) {
                values.add(new LogItemNutrientEntity(item.id(), value.code(), value.amount(), value.qualifier()));
            }
        }
        return values;
    }

    private static long millis(String instant) {
        return instant == null ? 0L : Instant.parse(instant).toEpochMilli();
    }
}
