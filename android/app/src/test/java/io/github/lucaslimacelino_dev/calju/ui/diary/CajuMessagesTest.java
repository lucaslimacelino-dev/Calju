package io.github.lucaslimacelino_dev.calju.ui.diary;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

public class CajuMessagesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 22);

    private static NutrientTotal total(String amount) {
        return new NutrientTotal(new BigDecimal(amount), true);
    }

    // Valores fictícios: só a escolha da fala importa.
    private static DayState day(LocalDate date, int entries, String kcal, String protein, Goal goal) {
        EntryRow row = new EntryRow("e", Occasion.LUNCH, 0L, "Registro", 1, BigDecimal.TEN, BigDecimal.ONE, "i", null);
        List<EntryRow> rows = entries == 0 ? Collections.emptyList() : Collections.nCopies(entries, row);
        return new DayState(date, total(kcal), total(protein), total("0"), total("0"), rows,
                Collections.emptyList(), goal);
    }

    private static Goal goal(String kcal, String protein) {
        return new Goal(TODAY.minusDays(5), new BigDecimal(kcal), protein == null ? null : new BigDecimal(protein),
                null, null);
    }

    @Test
    public void diaVazioOCajuDorme() {
        assertEquals(new CajuMessages.Line(CajuMessages.Mood.SLEEPING, CajuMessages.Kind.EMPTY_TODAY, 0, null),
                CajuMessages.forDay(day(TODAY, 0, "0", "0", null), TODAY));
        assertEquals(CajuMessages.Kind.EMPTY_PAST,
                CajuMessages.forDay(day(TODAY.minusDays(1), 0, "0", "0", null), TODAY).kind());
    }

    @Test
    public void diaPassadoESemMetaContamOsRegistros() {
        CajuMessages.Line past = CajuMessages.forDay(day(TODAY.minusDays(2), 3, "900", "20", goal("2000", null)), TODAY);
        assertEquals(CajuMessages.Kind.PAST_DAY, past.kind());
        assertEquals(3, past.count());

        assertEquals(CajuMessages.Kind.NO_GOAL, CajuMessages.forDay(day(TODAY, 2, "500", "10", null), TODAY).kind());
    }

    @Test
    public void comMetaMostraOQueFalta() {
        CajuMessages.Line line = CajuMessages.forDay(day(TODAY, 2, "266", "4.8", goal("2000", "90")), TODAY);

        assertEquals(CajuMessages.Mood.HAPPY, line.mood());
        assertEquals(CajuMessages.Kind.REMAINING, line.kind());
        assertEquals(0, new BigDecimal("1734").compareTo(line.kcal()));
    }

    @Test
    public void metaDeProteinaCompletaComemora() {
        CajuMessages.Line line = CajuMessages.forDay(day(TODAY, 4, "1500", "95", goal("2000", "90")), TODAY);

        assertEquals(CajuMessages.Mood.CELEBRATING, line.mood());
        assertEquals(CajuMessages.Kind.PROTEIN_DONE, line.kind());
    }

    @Test
    public void acimaDaMetaNaoEhBronca() {
        CajuMessages.Line line = CajuMessages.forDay(day(TODAY, 5, "2180", "95", goal("2000", "90")), TODAY);

        assertEquals(CajuMessages.Mood.HAPPY, line.mood());
        assertEquals(CajuMessages.Kind.OVER_GOAL, line.kind());
    }
}
