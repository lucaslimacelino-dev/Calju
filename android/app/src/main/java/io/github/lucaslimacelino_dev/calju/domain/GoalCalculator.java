package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Estima a meta diária a partir das respostas do questionário inicial.
 *
 * <ul>
 *   <li>Gasto em repouso: equação de Mifflin-St Jeor (Mifflin et al., 1990), a mais precisa entre as
 *       equações comparadas por Frankenfield et al. (2005), mas ainda uma estimativa.</li>
 *   <li>Gasto do dia: repouso × nível de atividade física (PAL). Usa o meio de cada faixa do relatório
 *       FAO/WHO/UNU "Human energy requirements" (2001, tabela 5.3): 1,40–1,69, 1,70–1,99 e 2,00–2,40.</li>
 *   <li>Perder peso: déficit de 500 ou 750 kcal/dia, as opções do guia AHA/ACC/TOS de 2013.
 *       Ganhar peso: +300 kcal/dia (decisão do app, sem referência oficial).</li>
 *   <li>A meta nunca fica abaixo do gasto em repouso.</li>
 *   <li>Macronutrientes: 20% proteína, 50% carboidrato e 30% gordura das calorias, dentro das faixas
 *       das National Academies (proteína 10–35%, carboidrato 45–65%, gordura 20–35%).</li>
 *   <li>Ganhar massa muscular (quem faz treino de força): +10% sobre o gasto do dia; proteína de
 *       2,0 g por kg de peso, limitada a 35% das calorias; gordura 25% das calorias; carboidrato o
 *       restante. Dentro das faixas de Iraki et al. (2019, Sports 7:154: +10–20% para iniciantes e
 *       intermediários, +5–10% para avançados; proteína 1,6–2,2 g/kg; gordura 0,5–1,5 g/kg, em
 *       geral 20–35% das calorias) e de Morton et al. (2018, Br J Sports Med 52:376: ganho de massa
 *       magra estabiliza perto de 1,6 g/kg, com intervalo até 2,2 g/kg).</li>
 * </ul>
 */
public final class GoalCalculator {

    public enum Sex {
        FEMALE,
        MALE
    }

    public enum Activity {
        /** Trabalho sentado ou em pé, sem exercício regular. */
        LIGHT(new BigDecimal("1.55")),
        /** Rotina que gasta mais energia, ou cerca de uma hora de exercício moderado por dia. */
        ACTIVE(new BigDecimal("1.85")),
        /** Trabalho pesado ou várias horas de esforço intenso por dia. */
        VIGOROUS(new BigDecimal("2.20"));

        final BigDecimal factor;

        Activity(BigDecimal factor) {
            this.factor = factor;
        }
    }

    public enum Objective {
        LOSE,
        KEEP,
        GAIN,
        /** Ganhar massa muscular com treino de força. */
        MUSCLE
    }

    public enum Pace {
        SLOW(500),
        MODERATE(750);

        final int deficitKcal;

        Pace(int deficitKcal) {
            this.deficitKcal = deficitKcal;
        }
    }

    /** Idades em que a equação foi desenvolvida (adultos de 19 a 78 anos), com folga para 18 e 80. */
    public static final int MIN_AGE = 18;
    public static final int MAX_AGE = 80;

    private static final int GAIN_SURPLUS_KCAL = 300;
    private static final BigDecimal MUSCLE_SURPLUS = new BigDecimal("1.10");
    private static final BigDecimal MUSCLE_PROTEIN_PER_KG = new BigDecimal("2.0");
    private static final BigDecimal MUSCLE_PROTEIN_MAX_SHARE = new BigDecimal("0.35");
    private static final BigDecimal MUSCLE_FAT_SHARE = new BigDecimal("0.25");
    private static final BigDecimal PROTEIN_SHARE = new BigDecimal("0.20");
    private static final BigDecimal CARBOHYDRATE_SHARE = new BigDecimal("0.50");
    private static final BigDecimal FAT_SHARE = new BigDecimal("0.30");
    private static final BigDecimal KCAL_PER_G_PROTEIN_OR_CARB = new BigDecimal("4");
    private static final BigDecimal KCAL_PER_G_FAT = new BigDecimal("9");

    public record Answers(Objective objective, Pace pace, Sex sex, int age, BigDecimal heightCm,
                          BigDecimal weightKg, Activity activity) {

        public Answers {
            Objects.requireNonNull(objective, "objective");
            Objects.requireNonNull(sex, "sex");
            Objects.requireNonNull(heightCm, "heightCm");
            Objects.requireNonNull(weightKg, "weightKg");
            Objects.requireNonNull(activity, "activity");
            if (objective == Objective.LOSE) {
                Objects.requireNonNull(pace, "pace");
            }
            if (age < MIN_AGE || age > MAX_AGE) {
                throw new IllegalArgumentException("Idade fora da faixa da equação: " + age);
            }
            if (heightCm.signum() <= 0 || weightKg.signum() <= 0) {
                throw new IllegalArgumentException("Altura e peso devem ser maiores que zero");
            }
        }
    }

    /**
     * {@code limitedByResting}: o déficit foi reduzido para a meta não ficar abaixo do gasto em repouso.
     */
    public record Plan(int restingKcal, int dailyKcal, int targetKcal, int proteinG, int carbohydrateG,
                       int fatG, boolean limitedByResting) {

        public Goal toGoal(java.time.LocalDate effectiveFrom) {
            return new Goal(effectiveFrom, BigDecimal.valueOf(targetKcal), BigDecimal.valueOf(proteinG),
                    BigDecimal.valueOf(carbohydrateG), BigDecimal.valueOf(fatG));
        }
    }

    private GoalCalculator() {
    }

    public static Plan plan(Answers answers) {
        BigDecimal resting = restingKcal(answers);
        BigDecimal daily = resting.multiply(answers.activity().factor);
        BigDecimal target = switch (answers.objective()) {
            case LOSE -> daily.subtract(BigDecimal.valueOf(answers.pace().deficitKcal));
            case KEEP -> daily;
            case GAIN -> daily.add(BigDecimal.valueOf(GAIN_SURPLUS_KCAL));
            case MUSCLE -> daily.multiply(MUSCLE_SURPLUS);
        };
        boolean limited = target.compareTo(resting) < 0;
        if (limited) {
            target = resting;
        }
        int targetKcal = roundTo10(target);
        BigDecimal kcal = BigDecimal.valueOf(targetKcal);
        if (answers.objective() == Objective.MUSCLE) {
            return musclePlan(answers, roundTo10(resting), roundTo10(daily), targetKcal, limited);
        }
        return new Plan(roundTo10(resting), roundTo10(daily), targetKcal,
                grams(kcal, PROTEIN_SHARE, KCAL_PER_G_PROTEIN_OR_CARB),
                grams(kcal, CARBOHYDRATE_SHARE, KCAL_PER_G_PROTEIN_OR_CARB),
                grams(kcal, FAT_SHARE, KCAL_PER_G_FAT), limited);
    }

    private static Plan musclePlan(Answers answers, int resting, int daily, int targetKcal, boolean limited) {
        BigDecimal kcal = BigDecimal.valueOf(targetKcal);
        BigDecimal byWeight = answers.weightKg().multiply(MUSCLE_PROTEIN_PER_KG);
        BigDecimal ceiling = kcal.multiply(MUSCLE_PROTEIN_MAX_SHARE).divide(KCAL_PER_G_PROTEIN_OR_CARB, 4,
                RoundingMode.HALF_UP);
        int protein = byWeight.min(ceiling).setScale(0, RoundingMode.HALF_UP).intValueExact();
        int fat = grams(kcal, MUSCLE_FAT_SHARE, KCAL_PER_G_FAT);
        BigDecimal rest = kcal.subtract(BigDecimal.valueOf(protein).multiply(KCAL_PER_G_PROTEIN_OR_CARB))
                .subtract(BigDecimal.valueOf(fat).multiply(KCAL_PER_G_FAT));
        int carbohydrate = rest.max(BigDecimal.ZERO).divide(KCAL_PER_G_PROTEIN_OR_CARB, 0, RoundingMode.HALF_UP)
                .intValueExact();
        return new Plan(resting, daily, targetKcal, protein, carbohydrate, fat, limited);
    }

    /** Mifflin-St Jeor: 10 × peso (kg) + 6,25 × altura (cm) − 5 × idade + 5 (homens) ou − 161 (mulheres). */
    static BigDecimal restingKcal(Answers answers) {
        BigDecimal base = BigDecimal.TEN.multiply(answers.weightKg())
                .add(new BigDecimal("6.25").multiply(answers.heightCm()))
                .subtract(BigDecimal.valueOf(5L * answers.age()));
        return base.add(BigDecimal.valueOf(answers.sex() == Sex.MALE ? 5 : -161));
    }

    private static int grams(BigDecimal kcal, BigDecimal share, BigDecimal kcalPerGram) {
        return kcal.multiply(share).divide(kcalPerGram, 0, RoundingMode.HALF_UP).intValueExact();
    }

    private static int roundTo10(BigDecimal kcal) {
        return kcal.divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP).multiply(BigDecimal.TEN).intValueExact();
    }
}
