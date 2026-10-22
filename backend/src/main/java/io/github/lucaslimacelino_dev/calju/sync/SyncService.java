package io.github.lucaslimacelino_dev.calju.sync;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lucaslimacelino_dev.calju.common.BadRequestException;
import io.github.lucaslimacelino_dev.calju.diary.LogEntryRepository;
import io.github.lucaslimacelino_dev.calju.diary.dto.LogEntryResponse;
import io.github.lucaslimacelino_dev.calju.food.FoodRepository;
import io.github.lucaslimacelino_dev.calju.food.dto.FoodResponse;
import io.github.lucaslimacelino_dev.calju.goal.GoalRepository;
import io.github.lucaslimacelino_dev.calju.goal.dto.GoalResponse;
import io.github.lucaslimacelino_dev.calju.meal.MealRepository;
import io.github.lucaslimacelino_dev.calju.meal.dto.MealResponse;
import io.github.lucaslimacelino_dev.calju.sync.dto.SyncPullResponse;

/** Leitura incremental: tudo do usuário com número de sequência maior que o cursor, em páginas. */
@Service
public class SyncService {

    static final int DEFAULT_LIMIT = 500;
    static final int MAX_LIMIT = 1000;

    /** Uma linha por registro alterado, das quatro tabelas, em ordem de sequência. */
    private static final String CHANGES = """
            select kind, id, change_seq from (
                select 'GOAL' as kind, id, change_seq from nutrition_goals where owner_user_id = ? and change_seq > ?
                union all
                select 'FOOD', id, change_seq from foods where owner_user_id = ? and change_seq > ?
                union all
                select 'MEAL', id, change_seq from meals where owner_user_id = ? and change_seq > ?
                union all
                select 'ENTRY', id, change_seq from log_entries where owner_user_id = ? and change_seq > ?
            ) changes
            order by change_seq
            limit ?
            """;

    private record Change(String kind, UUID id, long seq) {
    }

    private final JdbcTemplate jdbc;
    private final GoalRepository goals;
    private final FoodRepository foods;
    private final MealRepository meals;
    private final LogEntryRepository entries;

    public SyncService(JdbcTemplate jdbc, GoalRepository goals, FoodRepository foods, MealRepository meals,
                       LogEntryRepository entries) {
        this.jdbc = jdbc;
        this.goals = goals;
        this.foods = foods;
        this.meals = meals;
        this.entries = entries;
    }

    @Transactional(readOnly = true)
    public SyncPullResponse pull(UUID ownerId, long cursor, int limit) {
        if (cursor < 0 || limit < 1 || limit > MAX_LIMIT) {
            throw new BadRequestException("cursor deve ser ≥ 0 e limit entre 1 e " + MAX_LIMIT + ".");
        }
        // Busca um a mais que o limite só para saber se há outra página.
        List<Change> changes = jdbc.query(CHANGES,
                (rs, row) -> new Change(rs.getString("kind"), rs.getObject("id", UUID.class), rs.getLong("change_seq")),
                ownerId, cursor, ownerId, cursor, ownerId, cursor, ownerId, cursor, limit + 1);
        boolean hasMore = changes.size() > limit;
        List<Change> page = hasMore ? changes.subList(0, limit) : changes;
        long nextCursor = page.isEmpty() ? cursor : page.get(page.size() - 1).seq();

        return new SyncPullResponse(nextCursor, hasMore,
                goals.findAllById(ids(page, "GOAL")).stream().map(GoalResponse::from).toList(),
                foods.findAllById(ids(page, "FOOD")).stream().map(FoodResponse::from).toList(),
                meals.findAllById(ids(page, "MEAL")).stream().map(MealResponse::from).toList(),
                entries.findAllById(ids(page, "ENTRY")).stream().map(LogEntryResponse::from).toList());
    }

    private static List<UUID> ids(List<Change> changes, String kind) {
        List<UUID> ids = new ArrayList<>();
        for (Change change : changes) {
            if (change.kind().equals(kind)) {
                ids.add(change.id());
            }
        }
        return ids;
    }
}
