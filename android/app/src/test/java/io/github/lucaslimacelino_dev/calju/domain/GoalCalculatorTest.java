package io.github.lucaslimacelino_dev.calju.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.math.BigDecimal;

import org.junit.Test;

import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Activity;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Answers;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Objective;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Pace;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Plan;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Sex;

public class GoalCalculatorTest {

    private static Answers answers(Objective objective, Pace pace, Sex sex, int age, String height, String weight,
                                   Activity activity) {
        return new Answers(objective, pace, sex, age, new BigDecimal(height), new BigDecimal(weight), activity);
    }

    @Test
    public void gastoEmRepousoSegueAEquacaoDeMifflinStJeor() {
        // Mulher, 30 anos, 165 cm, 60 kg: 10×60 + 6,25×165 − 5×30 − 161 = 1320,25
        assertEquals(0, new BigDecimal("1320.25").compareTo(GoalCalculator.restingKcal(
                answers(Objective.KEEP, null, Sex.FEMALE, 30, "165", "60", Activity.LIGHT))));
        // Homem, 30 anos, 180 cm, 80 kg: 10×80 + 6,25×180 − 5×30 + 5 = 1780
        assertEquals(0, new BigDecimal("1780").compareTo(GoalCalculator.restingKcal(
                answers(Objective.KEEP, null, Sex.MALE, 30, "180", "80", Activity.LIGHT))));
    }

    @Test
    public void manterUsaOGastoDoDiaEDivideOsMacros() {
        Plan plan = GoalCalculator.plan(answers(Objective.KEEP, null, Sex.MALE, 30, "180", "80", Activity.LIGHT));

        assertEquals(1780, plan.restingKcal());
        assertEquals(2760, plan.dailyKcal());          // 1780 × 1,55 = 2759 → 2760
        assertEquals(2760, plan.targetKcal());
        assertEquals(138, plan.proteinG());            // 20% de 2760 / 4
        assertEquals(345, plan.carbohydrateG());       // 50% de 2760 / 4
        assertEquals(92, plan.fatG());                 // 30% de 2760 / 9
        assertFalse(plan.limitedByResting());
    }

    @Test
    public void perderTiraODeficitEscolhido() {
        Answers devagar = answers(Objective.LOSE, Pace.SLOW, Sex.MALE, 30, "180", "80", Activity.ACTIVE);
        Answers moderado = answers(Objective.LOSE, Pace.MODERATE, Sex.MALE, 30, "180", "80", Activity.ACTIVE);

        assertEquals(2790, GoalCalculator.plan(devagar).targetKcal());   // 1780 × 1,85 = 3293 − 500
        assertEquals(2540, GoalCalculator.plan(moderado).targetKcal());  // 3293 − 750
    }

    @Test
    public void ganharSomaUmPouco() {
        Plan plan = GoalCalculator.plan(answers(Objective.GAIN, null, Sex.FEMALE, 30, "165", "60", Activity.LIGHT));

        assertEquals(2050, plan.dailyKcal());   // 1320,25 × 1,55 = 2046
        assertEquals(2350, plan.targetKcal());  // + 300
    }

    @Test
    public void ganharMassaMuscularUsaProteinaPorQuiloEMaisDezPorCento() {
        Plan plan = GoalCalculator.plan(answers(Objective.MUSCLE, null, Sex.MALE, 30, "180", "80", Activity.LIGHT));

        assertEquals(3030, plan.targetKcal());   // 1780 × 1,55 = 2759 × 1,10 = 3034,9 → 3030
        assertEquals(160, plan.proteinG());      // 2 g × 80 kg
        assertEquals(84, plan.fatG());           // 25% de 3030 / 9 = 84,2
        assertEquals(409, plan.carbohydrateG()); // (3030 − 160×4 − 84×9) / 4 = 408,5
    }

    @Test
    public void proteinaDaMassaMuscularNaoPassaDe35PorCentoDasCalorias() {
        // Mulher 80 anos, 150 cm, 120 kg: repouso 1576,5 × 1,55 × 1,10 = 2687,9 → 2690 kcal.
        // 2 g × 120 kg = 240 g passaria do teto: 35% de 2690 / 4 = 235,4 → 235 g.
        Plan plan = GoalCalculator.plan(answers(Objective.MUSCLE, null, Sex.FEMALE, 80, "150", "120",
                Activity.LIGHT));

        assertEquals(2690, plan.targetKcal());
        assertEquals(235, plan.proteinG());
        assertEquals(75, plan.fatG());
        assertEquals(269, plan.carbohydrateG());
    }

    @Test
    public void metaNuncaFicaAbaixoDoGastoEmRepouso() {
        // Mulher 70 anos, 150 cm, 45 kg, rotina leve: repouso 806 kcal, dia 1249, menos 750 = 499.
        Plan plan = GoalCalculator.plan(answers(Objective.LOSE, Pace.MODERATE, Sex.FEMALE, 70, "150", "45",
                Activity.LIGHT));

        assertTrue(plan.limitedByResting());
        assertEquals(plan.restingKcal(), plan.targetKcal());
    }

    @Test
    public void idadeForaDaFaixaDaEquacaoNaoCalcula() {
        assertThrows(IllegalArgumentException.class,
                () -> answers(Objective.KEEP, null, Sex.MALE, 16, "170", "60", Activity.LIGHT));
        assertThrows(IllegalArgumentException.class,
                () -> answers(Objective.KEEP, null, Sex.MALE, 85, "170", "60", Activity.LIGHT));
    }

    @Test
    public void perderExigeRitmo() {
        assertThrows(NullPointerException.class,
                () -> answers(Objective.LOSE, null, Sex.MALE, 30, "170", "60", Activity.LIGHT));
    }
}
