package io.github.lucaslimacelino_dev.calju.data.repository;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @WorkerThread
    public EntryWithItems entry(String entryId) {
        return diary.entry(entryId);
    }

    /** Cria um lançamento com um alimento do catálogo. Retorna o id do lançamento. */
    @WorkerThread
    public String addFood(LocalDate date, Occasion occasion, String foodId, BigDecimal grams) {
        return addItems(date, occasion, null, null, Collections.singletonList(ItemDraft.of(foodId, grams)));
    }

    /**
     * Cria um lançamento com vários itens (ex.: uma refeição salva, com os ajustes do dia).
     * Cada item ganha sua cópia dos nutrientes. Retorna o id do lançamento.
     */
    @WorkerThread
    public String addItems(LocalDate date, Occasion occasion, String title, String sourceMealId,
                           List<ItemDraft> items) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("O registro precisa de pelo menos um alimento");
        }
        long now = clock.millis();
        String entryId = UUID.randomUUID().toString();
        database.runInTransaction(() -> {
            diary.insertEntry(new LogEntryEntity(entryId, date, occasion, title, sourceMealId, now, now, null, 1,
                    LogEntryEntity.SYNC_PENDING));
            for (int i = 0; i < items.size(); i++) {
                insertSnapshotItem(entryId, items.get(i), i);
            }
        });
        return entryId;
    }

    /**
     * Edita um lançamento: itens com {@code itemId} mantêm sua cópia dos nutrientes e só mudam de
     * quantidade; itens sem {@code itemId} são novos (copiados do catálogo agora); itens que não
     * vierem na lista são apagados. Sem itens, o lançamento é removido (pode ser desfeito).
     */
    @WorkerThread
    public void updateEntry(String entryId, Occasion occasion, List<ItemDraft> items) {
        database.runInTransaction(() -> {
            // Lido antes de inserir os novos, para que eles não sejam confundidos com removidos.
            Set<String> removed = new HashSet<>(diary.itemIdsOf(entryId));
            for (int i = 0; i < items.size(); i++) {
                ItemDraft item = items.get(i);
                if (item.itemId() == null) {
                    insertSnapshotItem(entryId, item, i);
                } else {
                    removed.remove(item.itemId());
                    diary.updateItem(item.itemId(), item.grams(), i);
                }
            }
            for (String itemId : removed) {
                diary.deleteItem(itemId);
            }
            long now = clock.millis();
            diary.updateEntryOccasion(entryId, occasion);
            if (items.isEmpty()) {
                diary.markDeleted(entryId, now);
            }
            diary.touchEntry(entryId, now);
        });
    }

    private void insertSnapshotItem(String entryId, ItemDraft draft, int position) {
        FoodEntity food = catalog.food(draft.foodId());
        if (food == null) {
            throw new IllegalArgumentException("Alimento não encontrado: " + draft.foodId());
        }
        FoodPortionEntity portion = catalog.defaultPortionOf(draft.foodId());
        String itemId = UUID.randomUUID().toString();
        List<LogItemNutrientEntity> snapshot = new ArrayList<>();
        for (FoodNutrientEntity value : catalog.nutrientsOf(draft.foodId())) {
            snapshot.add(new LogItemNutrientEntity(itemId, value.nutrientCode, value.amountPer100g, value.qualifier));
        }
        diary.insertItem(new LogItemEntity(itemId, entryId, draft.foodId(), food.name, null, food.sourceId,
                draft.grams(), portion == null ? null : portion.label, position));
        diary.insertItemNutrients(snapshot);
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
