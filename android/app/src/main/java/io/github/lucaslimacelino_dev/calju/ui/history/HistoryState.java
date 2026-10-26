package io.github.lucaslimacelino_dev.calju.ui.history;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Uma semana do histórico (segunda a domingo).
 *
 * @param bars     os 7 dias, de segunda a domingo
 * @param goalKcal meta que vale no último dia já vivido da semana; nula se não havia meta
 * @param average  média dos dias com registro, sem contar hoje; nula se não houver nenhum
 * @param streak   dias seguidos com registro até hoje (ou até ontem, se hoje ainda está vazio)
 * @param days     dias da semana com registro, do mais recente para o mais antigo
 */
public record HistoryState(LocalDate weekStart, List<WeekBar> bars, BigDecimal goalKcal, WeekAverage average,
                           int streak, boolean streakIncludesToday, List<HistoryDay> days,
                           boolean hasPreviousWeek, boolean hasNextWeek) {

    /** {@code kcal} nula quando o dia não tem registro. */
    public record WeekBar(LocalDate date, BigDecimal kcal, boolean today, boolean future) {
    }

    /** {@code complete} é falso se algum alimento não tinha algum desses valores medidos. */
    public record WeekAverage(BigDecimal kcal, BigDecimal protein, BigDecimal carbohydrate, BigDecimal fat,
                              int days, boolean complete) {
    }
}
