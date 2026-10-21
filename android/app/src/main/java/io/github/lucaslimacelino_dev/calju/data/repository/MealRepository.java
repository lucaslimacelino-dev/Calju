package io.github.lucaslimacelino_dev.calju.data.repository;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.LogEntryEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealDao;
import io.github.lucaslimacelino_dev.calju.data.local.MealEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealItemEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealWithItems;

/** Refeições salvas (moldes). Alterar um molde não altera registros já feitos no diário. */
public class MealRepository {

    private final CaljuDatabase database;
    private final MealDao dao;
    private final Clock clock;

    public MealRepository(CaljuDatabase database, Clock clock) {
        this.database = database;
        this.dao = database.mealDao();
        this.clock = clock;
    }

    public LiveData<List<MealWithItems>> observeAll() {
        return dao.observeAll();
    }

    @WorkerThread
    public List<MealWithItems> all() {
        return dao.all();
    }

    @WorkerThread
    public MealWithItems byId(String id) {
        return dao.byId(id);
    }

    @WorkerThread
    public String create(String name, List<ItemDraft> items) {
        String cleanName = requireName(name);
        requireItems(items);
        String id = UUID.randomUUID().toString();
        long now = clock.millis();
        database.runInTransaction(() -> {
            dao.insertMeal(new MealEntity(id, cleanName, now, now, null, 1, LogEntryEntity.SYNC_PENDING));
            dao.insertItems(toEntities(id, items));
        });
        return id;
    }

    /** Substitui nome e itens do molde. */
    @WorkerThread
    public void update(String id, String name, List<ItemDraft> items) {
        String cleanName = requireName(name);
        requireItems(items);
        database.runInTransaction(() -> {
            dao.rename(id, cleanName);
            dao.deleteItems(id);
            dao.insertItems(toEntities(id, items));
            dao.touch(id, clock.millis());
        });
    }

    /** Cria uma cópia independente, com novos identificadores. */
    @WorkerThread
    public String duplicate(String id, String copyName) {
        MealWithItems original = dao.byId(id);
        List<ItemDraft> items = new ArrayList<>();
        for (MealItemEntity item : original.items) {
            items.add(ItemDraft.of(item.foodId, item.quantityG));
        }
        return create(copyName, items);
    }

    @WorkerThread
    public void delete(String id) {
        database.runInTransaction(() -> {
            long now = clock.millis();
            dao.markDeleted(id, now);
            dao.touch(id, now);
        });
    }

    @WorkerThread
    public void restore(String id) {
        database.runInTransaction(() -> {
            dao.clearDeleted(id);
            dao.touch(id, clock.millis());
        });
    }

    private static List<MealItemEntity> toEntities(String mealId, List<ItemDraft> items) {
        List<MealItemEntity> entities = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            ItemDraft item = items.get(i);
            entities.add(new MealItemEntity(UUID.randomUUID().toString(), mealId, item.foodId(), item.grams(), i));
        }
        return entities;
    }

    private static String requireName(String name) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("A refeição precisa de um nome");
        }
        return clean;
    }

    private static void requireItems(List<ItemDraft> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("A refeição precisa de pelo menos um alimento");
        }
    }
}
