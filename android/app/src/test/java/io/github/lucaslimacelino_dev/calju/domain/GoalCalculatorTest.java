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
    public void manterUsaOGastoDoDiaEProteinaDe1v6PorQuilo() {
        Plan plan = GoalCalculator.plan(answers(Objective.KEEP, null, Sex.MALE, 30, "180", "80", Activity.LIGHT));

        assertEquals(1780, plan.restingKcal());
        assertEquals(2270, plan.dailyKcal());          // 1780 × 1,275 = 2269,5
        assertEquals(2270, plan.targetKcal());
        assertEquals(128, plan.proteinG());            // 1,6 g × 80 kg
        assertEquals(63, plan.fatG());                 // 25% de 2270 / 9 = 63,1
        assertEquals(298, plan.carbohydrateG());       // (2270 − 128×4 − 63×9) / 4 = 297,75
        assertFalse(plan.limitedByResting());
        assertFalse(plan.proteinLimited());
    }

    @Test
    public void fatoresDaRotinaSaoMetadeDoAcrescimo() {
        assertEquals(2540, GoalCalculator.plan(answers(Objective.KEEP, null, Sex.MALE, 30, "180", "80",
                Activity.ACTIVE)).dailyKcal());        // 1780 × 1,425 = 2536,5
        assertEquals(2850, GoalCalculator.plan(answers(Objective.KEEP, null, Sex.MALE, 30, "180", "80",
                Activity.VIGOROUS)).dailyKcal());      // 1780 × 1,60 = 2848
    }

    @Test
    public void perderTiraODeficitEMantemProteinaAlta() {
        Plan devagar = GoalCalculator.plan(answers(Objective.LOSE, Pace.SLOW, Sex.MALE, 30, "180", "80",
                Activity.ACTIVE));
        Plan moderado = GoalCalculator.plan(answers(Objective.LOSE, Pace.MODERATE, Sex.MALE, 30, "180", "80",
                Activity.ACTIVE));

        assertEquals(2040, devagar.targetKcal());    // 2536,5 − 500
        assertEquals(1790, moderado.targetKcal());   // 2536,5 − 750
        assertEquals(160, devagar.proteinG());       // 2,0 g × 80 kg
        assertFalse(devagar.proteinLimited());
        assertEquals(157, moderado.proteinG());      // teto: 35% de 1790 / 4 = 156,6
        assertTrue(moderado.proteinLimited());
    }

    @Test
    public void ganharSomaCincoPorCentoComProteinaAlta() {
        Plan plan = GoalCalculator.plan(answers(Objective.GAIN, null, Sex.MALE, 30, "180", "80", Activity.LIGHT));

        assertEquals(2380, plan.targetKcal());       // 2269,5 × 1,05 = 2383
        assertEquals(160, plan.proteinG());          // 2,0 g × 80 kg
        assertEquals(66, plan.fatG());               // 25% de 2380 / 9 = 66,1
        assertEquals(287, plan.carbohydrateG());     // (2380 − 640 − 594) / 4 = 286,5
    }

    @Test
    public void metaNuncaFicaAbaixoDoGastoEmRepouso() {
        // Mulher 70 anos, 150 cm, 45 kg: repouso 876,5; dia 1117,5; menos 750 = 367,5.
        Plan plan = GoalCalculator.plan(answers(Objective.LOSE, Pace.MODERATE, Sex.FEMALE, 70, "150", "45",
                Activity.LIGHT));

        assertTrue(plan.limitedByResting());
        assertEquals(plan.restingKcal(), plan.targetKcal());
    }

    @Test
    public void proteinaNaoPassaDe35PorCentoDasCalorias() {
        // Mulher 80 anos, 150 cm, 120 kg: meta presa no repouso, 1576,5 → 1580 kcal.
        // 2 g × 120 kg = 240 g passaria do teto: 35% de 1580 / 4 = 138,25 → 138 g.
        Plan plan = GoalCalculator.plan(answers(Objective.LOSE, Pace.SLOW, Sex.FEMALE, 80, "150", "120",
                Activity.LIGHT));

        assertEquals(1580, plan.targetKcal());
        assertEquals(138, plan.proteinG());
        assertTrue(plan.proteinLimited());
        assertEquals(44, plan.fatG());
        assertEquals(158, plan.carbohydrateG());
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
