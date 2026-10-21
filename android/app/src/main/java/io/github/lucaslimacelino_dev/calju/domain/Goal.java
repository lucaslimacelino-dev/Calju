package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

/**
 * Meta diária definida pelo usuário, válida a partir de {@code effectiveFrom}.
 * kcal é obrigatória; metas de macronutrientes são opcionais (nulas = sem meta).
 */
public record Goal(LocalDate effectiveFrom, BigDecimal kcal, BigDecimal proteinG, BigDecimal carbohydrateG,
                   BigDecimal fatG) {

    public Goal {
        Objects.requireNonNull(effectiveFrom, "effectiveFrom");
        Objects.requireNonNull(kcal, "kcal");
        if (kcal.signum() <= 0) {
            throw new IllegalArgumentException("Meta de kcal deve ser maior que zero: " + kcal);
        }
        requirePositiveOrNull(proteinG, "proteína");
        requirePositiveOrNull(carbohydrateG, "carboidrato");
        requirePositiveOrNull(fatG, "gordura");
    }

    /** A meta vigente num dia é a de vigência mais recente que não seja posterior ao dia. */
    public static Optional<Goal> forDate(Collection<Goal> goals, LocalDate date) {
        Objects.requireNonNull(date, "date");
        Goal current = null;
        for (Goal goal : goals) {
            boolean applies = !goal.effectiveFrom().isAfter(date);
            if (applies && (current == null || goal.effectiveFrom().isAfter(current.effectiveFrom()))) {
                current = goal;
            }
        }
        return Optional.ofNullable(current);
    }

    private static void requirePositiveOrNull(BigDecimal value, String name) {
        if (value != null && value.signum() <= 0) {
            throw new IllegalArgumentException("Meta de " + name + " deve ser maior que zero: " + value);
        }
    }
}
