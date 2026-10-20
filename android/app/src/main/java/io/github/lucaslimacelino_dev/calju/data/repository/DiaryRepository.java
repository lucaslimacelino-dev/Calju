package io.github.lucaslimacelino_dev.calju.data.repository;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogDao;
import io.github.lucaslimacelino_dev.calju.data.local.DiaryDao;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.FoodEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodNutrientEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodPortionEntity;
import io.github.lucaslimacelino_dev.calju.data.local.ItemWithNutrients;
import io.github.lucaslimacelino_dev.calju.data.local.LogEntryEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogItemEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogItemNutrientEntity;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.NutrientAmount;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import io.github.lucaslimacelino_dev.calju.domain.Qualifier;

/**
 * Diário alimentar. Ao registrar, copia nome e nutrientes por 100 g do catálogo: mudanças
 * posteriores no catálogo não alteram o passado, e a quantidade pode ser editada depois.
 */
public class DiaryRepository {

    private final CaljuDatabase database;
    private final DiaryDao diary;
    private final CatalogDao catalog;
    private final Clock clock;

    public DiaryRepository(CaljuDatabase database, Clock clock) {
        this.database = database;
        this.diary = database.diaryDao();
        this.catalog = database.catalogDao();
        this.clock = clock;
    }

    public LiveData<List<EntryWithItems>> observeDay(LocalDate date) {
        return diary.observeDay(date);
    }

    @WorkerThread
    public List<EntryWithItems> day(LocalDate date) {
        return diary.day(date);
    }

    /** Cria um lançamento com um alimento do catálogo. Retorna o id do lançamento. */
    @WorkerThread
    public String addFood(LocalDate date, Occasion occasion, String foodId, BigDecimal grams) {
        if (grams.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero: " + grams);
        }
        FoodEntity food = catalog.food(foodId);
        if (food == null) {
            throw new IllegalArgumentException("Alimento não encontrado: " + foodId);
        }
        FoodPortionEntity portion = catalog.defaultPortionOf(foodId);
        long now = clock.millis();
        String entryId = UUID.randomUUID().toString();
        String itemId = UUID.randomUUID().toString();

        List<LogItemNutrientEntity> snapshot = new ArrayList<>();
        for (FoodNutrientEntity value : catalog.nutrientsOf(foodId)) {
            snapshot.add(new LogItemNutrientEntity(itemId, value.nutrientCode, value.amountPer100g, value.qualifier));
        }

        database.runInTransaction(() -> {
            diary.insertEntry(new LogEntryEntity(entryId, date, occasion, null, null, now, now, null, 1,
                    LogEntryEntity.SYNC_PENDING));
            diary.insertItem(new LogItemEntity(itemId, entryId, foodId, food.name, null, food.sourceId, grams,
                    portion == null ? null : portion.label, 0));
            diary.insertItemNutrients(snapshot);
        });
        return entryId;
    }

    @WorkerThread
    public void updateItem(String itemId, BigDecimal grams, Occasion occasion) {
        if (grams.signum() <= 0) {
            throw new IllegalArgumentException("Quantidade deve ser maior que zero: " + grams);
        }
        database.runInTransaction(() -> {
            String entryId = diary.entryIdOf(itemId);
            diary.updateItemQuantity(itemId, grams);
            diary.updateEntryOccasion(entryId, occasion);
            diary.touchEntry(entryId, clock.millis());
        });
    }

    /** Exclusão lógica: o lançamento some do dia, mas pode ser restaurado (desfazer). */
    @WorkerThread
    public void removeEntry(String entryId) {
        database.runInTransaction(() -> {
            long now = clock.millis();
            diary.markDeleted(entryId, now);
            diary.touchEntry(entryId, now);
        });
    }

    @WorkerThread
    public void restoreEntry(String entryId) {
        database.runInTransaction(() -> {
            diary.clearDeleted(entryId);
            diary.touchEntry(entryId, clock.millis());
        });
    }

    /** Converte o item gravado (com os nutrientes copiados) para o domínio. */
    public static Intake toIntake(ItemWithNutrients item) {
        Map<String, NutrientAmount> nutrients = new HashMap<>();
        for (LogItemNutrientEntity value : item.nutrients) {
            nutrients.put(value.nutrientCode, "TRACE".equals(value.qualifier)
                    ? NutrientAmount.trace()
                    : new NutrientAmount(value.amountPer100g, Qualifier.VALUE));
        }
        return new Intake(new Food(item.item.foodNameSnap, nutrients, null), item.item.quantityG);
    }
}
