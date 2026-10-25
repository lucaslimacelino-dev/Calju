package io.github.lucaslimacelino_dev.calju.data.sync;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
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
import io.github.lucaslimacelino_dev.calju.domain.QuantityUnit;

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
        return new SyncDtos.FoodBody(food.name, food.brand, food.unit, nutrients(values),
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
                    it.quantityG, it.unit, it.portionLabelSnap, it.portionGramsSnap, values));
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
        food.unit = QuantityUnit.fromSymbol(dto.unit()).symbol();
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
            LogItemEntity entity = new LogItemEntity(item.id(), dto.id(), item.foodId(), item.foodName(),
                    item.brand(), item.source(), item.quantityG(), item.portionLabel(), i);
            entity.unit = QuantityUnit.fromSymbol(item.unit()).symbol();
            entity.portionGramsSnap = item.portionGrams();
            items.add(entity);
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

    // Mesmo conteúdo nos dois lados (ex.: reenvio depois de uma resposta perdida) não é conflito.
    // Excluído dos dois lados também não; excluído de um lado só é diferente.

    static boolean sameFood(SyncDtos.FoodBody local, boolean localDeleted, SyncDtos.Food remote) {
        return sameDeleted(localDeleted, remote.deletedAt()) && (localDeleted
                || (text(local.name()) + "|" + text(local.brand()) + "|" + unit(local.unit()) + "|"
                + nutrientsKey(local.nutrients()) + "|" + portionKey(local.portion())).equals(text(remote.name())
                + "|" + text(remote.brand()) + "|" + unit(remote.unit()) + "|" + nutrientsKey(remote.nutrients())
                + "|" + portionKey(remote.portion())));
    }

    static boolean sameMeal(SyncDtos.MealBody local, boolean localDeleted, SyncDtos.Meal remote) {
        return sameDeleted(localDeleted, remote.deletedAt()) && (localDeleted
                || (text(local.name()) + "|" + mealItemsKey(local.items()))
                .equals(text(remote.name()) + "|" + mealItemsKey(remote.items())));
    }

    static boolean sameGoal(NutritionGoalEntity local, SyncDtos.Goal remote) {
        return (local.effectiveFrom + "|" + number(local.kcal) + "|" + number(local.proteinG) + "|"
                + number(local.carbsG) + "|" + number(local.fatG)).equals(remote.effectiveFrom() + "|"
                + number(remote.kcal()) + "|" + number(remote.proteinG()) + "|" + number(remote.carbohydrateG())
                + "|" + number(remote.fatG()));
    }

    static boolean sameEntry(SyncDtos.LogEntryBody local, boolean localDeleted, SyncDtos.LogEntry remote) {
        return sameDeleted(localDeleted, remote.deletedAt()) && (localDeleted
                || entryKey(local.logDate(), local.occasion(), local.title(), local.sourceMealId(), local.createdAt(),
                local.items()).equals(entryKey(remote.logDate(), remote.occasion(), remote.title(),
                remote.sourceMealId(), remote.createdAt(), remote.items())));
    }

    private static boolean sameDeleted(boolean localDeleted, String remoteDeletedAt) {
        return localDeleted == (remoteDeletedAt != null);
    }

    private static String entryKey(String date, String occasion, String title, String sourceMealId, String createdAt,
                                   List<SyncDtos.LogItem> items) {
        StringBuilder key = new StringBuilder(date + "|" + occasion + "|" + text(title) + "|" + text(sourceMealId)
                + "|" + Instant.parse(createdAt).toEpochMilli());
        for (SyncDtos.LogItem item : items) {
            key.append("|").append(item.id()).append(",").append(item.foodId()).append(",")
                    .append(text(item.foodName())).append(",").append(text(item.brand())).append(",")
                    .append(text(item.source())).append(",").append(number(item.quantityG())).append(",")
                    .append(unit(item.unit())).append(",")
                    .append(text(item.portionLabel())).append(",").append(number(item.portionGrams())).append(",")
                    .append(nutrientsKey(item.nutrients()));
        }
        return key.toString();
    }

    private static String mealItemsKey(List<SyncDtos.MealItem> items) {
        StringBuilder key = new StringBuilder();
        for (SyncDtos.MealItem item : items) {
            key.append(item.id()).append(",").append(item.foodId()).append(",").append(number(item.quantityG()))
                    .append(";");
        }
        return key.toString();
    }

    private static String nutrientsKey(List<SyncDtos.NutrientValue> values) {
        TreeSet<String> sorted = new TreeSet<>();
        for (SyncDtos.NutrientValue value : values) {
            sorted.add(value.code() + "=" + number(value.amount()) + ":" + text(value.qualifier()));
        }
        return sorted.toString();
    }

    private static String portionKey(SyncDtos.Portion portion) {
        return portion == null ? "-" : text(portion.label()) + "=" + number(portion.grams());
    }

    /** 150 e 150.0000 são o mesmo número; vazio e nulo são o mesmo texto. */
    private static String number(BigDecimal value) {
        return value == null ? "-" : value.stripTrailingZeros().toPlainString();
    }

    private static String unit(String value) {
        return QuantityUnit.fromSymbol(value).symbol();
    }

    private static String text(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }

    private static long millis(String instant) {
        return instant == null ? 0L : Instant.parse(instant).toEpochMilli();
    }
}
