package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.github.lucaslimacelino_dev.calju.domain.Goal;

/**
 * Escolhe o humor e a fala do Caju no topo do Diário. Sem Android: testável no computador.
 * Passar da meta nunca vira bronca; o Caju só comemora o que o usuário definiu como meta.
 */
public final class CajuMessages {

    public enum Mood {
        HAPPY,
        CELEBRATING,
        THINKING,
        SLEEPING
    }

    public enum Kind {
        EMPTY_TODAY,
        EMPTY_PAST,
        PAST_DAY,
        NO_GOAL,
        REMAINING,
        PROTEIN_DONE,
        OVER_GOAL
    }

    /** {@code count} = registros do dia; {@code kcal} = kcal restantes (só em REMAINING). */
    public record Line(Mood mood, Kind kind, int count, BigDecimal kcal) {
    }

    private CajuMessages() {
    }

    public static Line forDay(DayState state, LocalDate today) {
        int count = state.entries().size();
        boolean isToday = state.date().equals(today);
        if (count == 0) {
            return new Line(Mood.SLEEPING, isToday ? Kind.EMPTY_TODAY : Kind.EMPTY_PAST, 0, null);
        }
        if (!isToday) {
            return new Line(Mood.HAPPY, Kind.PAST_DAY, count, null);
        }
        Goal goal = state.goal();
        if (goal == null) {
            return new Line(Mood.HAPPY, Kind.NO_GOAL, count, null);
        }
        BigDecimal remaining = goal.kcal().subtract(state.kcal().amount());
        if (remaining.signum() < 0) {
            return new Line(Mood.HAPPY, Kind.OVER_GOAL, count, null);
        }
        if (goal.proteinG() != null && state.protein().amount().compareTo(goal.proteinG()) >= 0) {
            return new Line(Mood.CELEBRATING, Kind.PROTEIN_DONE, count, null);
        }
        return new Line(Mood.HAPPY, Kind.REMAINING, count, remaining);
    }
}
