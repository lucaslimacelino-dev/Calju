package io.github.lucaslimacelino_dev.calju.data.remote.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Formato dos recursos na API (iguais aos DTOs do backend). Datas como texto ISO ("2026-10-22")
 * e instantes como texto ISO ("2026-10-22T12:40:00Z"). {@code version} no corpo enviado é a versão
 * do servidor que o app conhece (0 = novo).
 */
public final class SyncDtos {

    private SyncDtos() {
    }

    public record NutrientValue(String code, BigDecimal amount, String qualifier) {
    }

    public record Portion(String label, BigDecimal grams) {
    }

    public record GoalBody(String effectiveFrom, BigDecimal kcal, BigDecimal proteinG, BigDecimal carbohydrateG,
                           BigDecimal fatG, int version) {
    }

    public record Goal(String id, String effectiveFrom, BigDecimal kcal, BigDecimal proteinG,
                       BigDecimal carbohydrateG, BigDecimal fatG, int version, String updatedAt) {
    }

    public record FoodBody(String name, String brand, String unit, List<NutrientValue> nutrients, Portion portion,
                           int version) {
    }

    public record Food(String id, String name, String brand, String unit, List<NutrientValue> nutrients,
                       Portion portion, int version, String updatedAt, String deletedAt) {
    }

    public record MealItem(String id, String foodId, BigDecimal quantityG) {
    }

    public record MealBody(String name, List<MealItem> items, int version) {
    }

    public record Meal(String id, String name, List<MealItem> items, int version, String updatedAt,
                       String deletedAt) {
    }

    public record LogItem(String id, String foodId, String foodName, String brand, String source,
                          BigDecimal quantityG, String unit, String portionLabel, List<NutrientValue> nutrients) {
    }

    public record LogEntryBody(String logDate, String occasion, String title, String sourceMealId,
                               String createdAt, List<LogItem> items, int version) {
    }

    public record LogEntry(String id, String logDate, String occasion, String title, String sourceMealId,
                           String createdAt, List<LogItem> items, int version, String updatedAt,
                           String deletedAt) {
    }

    /** Resposta de GET /sync/pull. */
    public record Pull(long cursor, boolean hasMore, List<Goal> goals, List<Food> foods, List<Meal> meals,
                       List<LogEntry> logEntries) {
    }
}
