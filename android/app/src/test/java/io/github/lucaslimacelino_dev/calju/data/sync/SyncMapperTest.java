package io.github.lucaslimacelino_dev.calju.data.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.Test;

import io.github.lucaslimacelino_dev.calju.data.local.NutritionGoalEntity;
import io.github.lucaslimacelino_dev.calju.data.local.SyncStates;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.SyncDtos;

/** Quando a cópia do servidor tem o mesmo conteúdo da do aparelho (não é conflito de verdade). */
public class SyncMapperTest {

    private static final SyncDtos.NutrientValue KCAL = new SyncDtos.NutrientValue("ENERC", new BigDecimal("128"), "VALUE");

    private static SyncDtos.LogItem item(String grams, List<SyncDtos.NutrientValue> nutrients) {
        return new SyncDtos.LogItem("item-1", "arroz", "Arroz, tipo 1, cozido", null, "TACO-4", new BigDecimal(grams),
                null, nutrients);
    }

    private static SyncDtos.LogEntry remote(SyncDtos.LogItem item, String deletedAt) {
        return new SyncDtos.LogEntry("e-1", "2026-10-22", "LUNCH", null, null, "2026-10-22T12:40:00.000Z",
                List.of(item), 3, "2026-10-22T12:41:00Z", deletedAt);
    }

    private static SyncDtos.LogEntryBody local(SyncDtos.LogItem item) {
        return new SyncDtos.LogEntryBody("2026-10-22", "LUNCH", "", null, "2026-10-22T12:40:00Z", List.of(item), 0);
    }

    @Test
    public void registroIgualComEscalasETextosVaziosDiferentesEOMesmo() {
        SyncDtos.NutrientValue scaled = new SyncDtos.NutrientValue("ENERC", new BigDecimal("128.0000"), "VALUE");
        assertTrue(SyncMapper.sameEntry(local(item("150", List.of(KCAL))), false,
                remote(item("150.00", List.of(scaled)), null)));
    }

    @Test
    public void quantidadeOuNutrienteDiferenteEConflito() {
        assertFalse(SyncMapper.sameEntry(local(item("150", List.of(KCAL))), false,
                remote(item("200", List.of(KCAL)), null)));
        SyncDtos.NutrientValue other = new SyncDtos.NutrientValue("ENERC", new BigDecimal("130"), "VALUE");
        assertFalse(SyncMapper.sameEntry(local(item("150", List.of(KCAL))), false,
                remote(item("150", List.of(other)), null)));
    }

    @Test
    public void excluidoDosDoisLadosEOMesmoDeUmLadoSoNao() {
        SyncDtos.LogEntryBody body = local(item("150", List.of(KCAL)));
        assertTrue(SyncMapper.sameEntry(body, true, remote(item("999", List.of()), "2026-10-22T13:00:00Z")));
        assertFalse(SyncMapper.sameEntry(body, true, remote(item("150", List.of(KCAL)), null)));
        assertFalse(SyncMapper.sameEntry(body, false, remote(item("150", List.of(KCAL)), "2026-10-22T13:00:00Z")));
    }

    @Test
    public void ordemDosNutrientesNaoImporta() {
        SyncDtos.NutrientValue protein = new SyncDtos.NutrientValue("PROCNT", new BigDecimal("2.5"), "VALUE");
        assertTrue(SyncMapper.sameEntry(local(item("150", List.of(KCAL, protein))), false,
                remote(item("150", List.of(protein, KCAL)), null)));
    }

    @Test
    public void metaComparaDataEValores() {
        NutritionGoalEntity goal = new NutritionGoalEntity("g", LocalDate.of(2026, 10, 22), new BigDecimal("2000"),
                new BigDecimal("90"), null, null, 0L, 1, SyncStates.PENDING);
        assertTrue(SyncMapper.sameGoal(goal, new SyncDtos.Goal("outro-id", "2026-10-22", new BigDecimal("2000.00"),
                new BigDecimal("90"), null, null, 2, "2026-10-22T12:00:00Z")));
        assertFalse(SyncMapper.sameGoal(goal, new SyncDtos.Goal("g", "2026-10-22", new BigDecimal("2000"),
                new BigDecimal("95"), null, null, 2, "2026-10-22T12:00:00Z")));
    }

    @Test
    public void refeicaoEAlimentoComparamConteudo() {
        SyncDtos.MealItem item = new SyncDtos.MealItem("i", "arroz", new BigDecimal("150"));
        assertTrue(SyncMapper.sameMeal(new SyncDtos.MealBody("Marmita", List.of(item), 1), false,
                new SyncDtos.Meal("m", "Marmita", List.of(new SyncDtos.MealItem("i", "arroz", new BigDecimal("150.0"))),
                        2, "2026-10-22T12:00:00Z", null)));
        assertFalse(SyncMapper.sameMeal(new SyncDtos.MealBody("Marmita", List.of(item), 1), false,
                new SyncDtos.Meal("m", "Marmita 2", List.of(item), 2, "2026-10-22T12:00:00Z", null)));
        SyncDtos.Portion portion = new SyncDtos.Portion("1 xícara", new BigDecimal("40"));
        assertTrue(SyncMapper.sameFood(new SyncDtos.FoodBody("Granola", null, List.of(KCAL), portion, 1), false,
                new SyncDtos.Food("f", "Granola", "", List.of(KCAL), portion, 2, "2026-10-22T12:00:00Z", null)));
        assertFalse(SyncMapper.sameFood(new SyncDtos.FoodBody("Granola", null, List.of(KCAL), portion, 1), false,
                new SyncDtos.Food("f", "Granola", null, List.of(KCAL), null, 2, "2026-10-22T12:00:00Z", null)));
    }

    @Test
    public void nomeDaCopiaCabeNoLimiteDoServidor() {
        assertEquals("Marmita (conflito)", SyncWriter.conflictName("Marmita"));
        String longName = "x".repeat(200);
        String copy = SyncWriter.conflictName(longName);
        assertEquals(200, copy.length());
        assertTrue(copy.endsWith(" (conflito)"));
    }
}
