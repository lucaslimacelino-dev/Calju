package io.github.lucaslimacelino_dev.calju.ui.history;

import java.math.BigDecimal;
import java.util.List;

/**
 * Dias com registro (mais recente primeiro) e a média de kcal dos últimos 7 dias antes de hoje
 * que têm registro. {@code weekAverage} é nula se nenhum desses dias tiver registro.
 */
public record HistoryState(List<HistoryDay> days, BigDecimal weekAverage, int weekDaysWithRecords,
                           boolean weekAverageComplete) {
}
