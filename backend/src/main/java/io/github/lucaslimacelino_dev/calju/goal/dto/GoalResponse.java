package io.github.lucaslimacelino_dev.calju.goal.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.goal.GoalEntity;

/** Meta como a API devolve. Não inclui o dono: o cliente só vê as próprias. */
public record GoalResponse(UUID id, LocalDate effectiveFrom, BigDecimal kcal, BigDecimal proteinG,
                           BigDecimal carbohydrateG, BigDecimal fatG, int version, Instant updatedAt) {

    public static GoalResponse from(GoalEntity goal) {
        return new GoalResponse(goal.getId(), goal.getEffectiveFrom(), goal.getKcal(), goal.getProteinG(),
                goal.getCarbohydrateG(), goal.getFatG(), goal.getVersion(), goal.getUpdatedAt());
    }
}
