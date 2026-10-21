package io.github.lucaslimacelino_dev.calju.data.repository;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.GoalDao;
import io.github.lucaslimacelino_dev.calju.data.local.LogEntryEntity;
import io.github.lucaslimacelino_dev.calju.data.local.NutritionGoalEntity;
import io.github.lucaslimacelino_dev.calju.domain.Goal;

/** Metas diárias. Salvar uma meta cria (ou corrige) a meta que vale a partir daquela data. */
public class GoalRepository {

    private final CaljuDatabase database;
    private final GoalDao dao;
    private final Clock clock;

    public GoalRepository(CaljuDatabase database, Clock clock) {
        this.database = database;
        this.dao = database.goalDao();
        this.clock = clock;
    }

    public LiveData<List<Goal>> observeAll() {
        return Transformations.map(dao.observeAll(), GoalRepository::toDomain);
    }

    @WorkerThread
    public List<Goal> all() {
        return toDomain(dao.all());
    }

    @WorkerThread
    public void save(Goal goal) {
        database.runInTransaction(() -> {
            long now = clock.millis();
            NutritionGoalEntity existing = dao.byEffectiveFrom(goal.effectiveFrom());
            if (existing == null) {
                dao.insert(new NutritionGoalEntity(UUID.randomUUID().toString(), goal.effectiveFrom(), goal.kcal(),
                        goal.proteinG(), goal.carbohydrateG(), goal.fatG(), now, 1, LogEntryEntity.SYNC_PENDING));
            } else {
                existing.kcal = goal.kcal();
                existing.proteinG = goal.proteinG();
                existing.carbsG = goal.carbohydrateG();
                existing.fatG = goal.fatG();
                existing.updatedAt = now;
                existing.version++;
                existing.syncState = LogEntryEntity.SYNC_PENDING;
                dao.update(existing);
            }
        });
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    private static List<Goal> toDomain(List<NutritionGoalEntity> entities) {
        List<Goal> goals = new ArrayList<>();
        for (NutritionGoalEntity e : entities) {
            goals.add(new Goal(e.effectiveFrom, e.kcal, e.proteinG, e.carbsG, e.fatG));
        }
        return goals;
    }
}
