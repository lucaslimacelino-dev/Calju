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
 *   <li>Gasto do dia: repouso × fator da rotina. Os fatores são decisão do app (pedido do autor, para
 *       não superestimar o gasto): metade do acréscimo do meio de cada faixa do relatório FAO/WHO/UNU
 *       "Human energy requirements" (2001, tabela 5.3; meios 1,55, 1,85 e 2,20), ou seja, 1,275,
 *       1,425 e 1,60. Ficam abaixo do mínimo medido pela FAO para sedentários (1,40): a meta tende a
 *       sair conservadora e pode ser ajustada no Perfil.</li>
 *   <li>Perder peso: déficit de 500 ou 750 kcal/dia, as opções do guia AHA/ACC/TOS de 2013.
 *       Ganhar peso: +5% sobre o gasto do dia, o menor superávit citado por Iraki et al. (2019,
 *       Sports 7:154: +5–10% para avançados, +10–20% para iniciantes), para ganhar músculo com pouca
 *       gordura.</li>
 *   <li>A meta nunca fica abaixo do gasto em repouso.</li>
 *   <li>Proteína por kg de peso: 2,0 g/kg para perder (preservar músculo) e para ganhar; 1,6 g/kg
 *       para manter. Faixa de 1,4–2,0 g/kg para quem se exercita (posicionamento da ISSN, Jäger et
 *       al. 2017) e ganho de massa magra que estabiliza perto de 1,6 g/kg (Morton et al. 2018).
 *       Limitada a 35% das calorias, o máximo da faixa das National Academies (proteína 10–35%).</li>
 *   <li>Gordura: 25% das calorias (faixa de 20–35%). Carboidrato: o restante.</li>
 * </ul>
 */
public final class GoalCalculator {

    public enum Sex {
        FEMALE,
        MALE
    }

    public enum Activity {
        /** Trabalho sentado ou em pé, sem exercício regular. */
        LIGHT(new BigDecimal("1.275")),
        /** Rotina que gasta mais energia, ou cerca de uma hora de exercício moderado por dia. */
        ACTIVE(new BigDecimal("1.425")),
        /** Trabalho pesado ou várias horas de esforço intenso por dia. */
        VIGOROUS(new BigDecimal("1.60"));

        final BigDecimal factor;

        Activity(BigDecimal factor) {
            this.factor = factor;
        }
    }

    public enum Objective {
        /** Perder peso preservando músculo (proteína alta). */
        LOSE(new BigDecimal("2.0")),
        KEEP(new BigDecimal("1.6")),
        /** Ganhar músculo com pouca gordura: superávit pequeno e proteína alta. */
        GAIN(new BigDecimal("2.0"));

        final BigDecimal proteinPerKg;

        Objective(BigDecimal proteinPerKg) {
            this.proteinPerKg = proteinPerKg;
        }
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

    private static final BigDecimal GAIN_SURPLUS = new BigDecimal("1.05");
    private static final BigDecimal PROTEIN_MAX_SHARE = new BigDecimal("0.35");
    private static final BigDecimal FAT_SHARE = new BigDecimal("0.25");
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
     * {@code proteinLimited}: a proteína por kg passaria de 35% das calorias e foi reduzida.
     */
    public record Plan(int restingKcal, int dailyKcal, int targetKcal, int proteinG, int carbohydrateG,
                       int fatG, boolean limitedByResting, boolean proteinLimited) {

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
            case GAIN -> daily.multiply(GAIN_SURPLUS);
        };
        boolean limited = target.compareTo(resting) < 0;
        if (limited) {
            target = resting;
        }
        int targetKcal = roundTo10(target);
        BigDecimal kcal = BigDecimal.valueOf(targetKcal);

        BigDecimal byWeight = answers.weightKg().multiply(answers.objective().proteinPerKg);
        BigDecimal ceiling = kcal.multiply(PROTEIN_MAX_SHARE).divide(KCAL_PER_G_PROTEIN_OR_CARB, 4,
                RoundingMode.HALF_UP);
        boolean proteinLimited = byWeight.compareTo(ceiling) > 0;
        int protein = byWeight.min(ceiling).setScale(0, RoundingMode.HALF_UP).intValueExact();
        int fat = kcal.multiply(FAT_SHARE).divide(KCAL_PER_G_FAT, 0, RoundingMode.HALF_UP).intValueExact();
        BigDecimal rest = kcal.subtract(BigDecimal.valueOf(protein).multiply(KCAL_PER_G_PROTEIN_OR_CARB))
                .subtract(BigDecimal.valueOf(fat).multiply(KCAL_PER_G_FAT));
        int carbohydrate = rest.max(BigDecimal.ZERO).divide(KCAL_PER_G_PROTEIN_OR_CARB, 0, RoundingMode.HALF_UP)
                .intValueExact();
        return new Plan(roundTo10(resting), roundTo10(daily), targetKcal, protein, carbohydrate, fat, limited,
                proteinLimited);
    }

    /** Mifflin-St Jeor: 10 × peso (kg) + 6,25 × altura (cm) − 5 × idade + 5 (homens) ou − 161 (mulheres). */
    static BigDecimal restingKcal(Answers answers) {
        BigDecimal base = BigDecimal.TEN.multiply(answers.weightKg())
                .add(new BigDecimal("6.25").multiply(answers.heightCm()))
                .subtract(BigDecimal.valueOf(5L * answers.age()));
        return base.add(BigDecimal.valueOf(answers.sex() == Sex.MALE ? 5 : -161));
    }

    private static int roundTo10(BigDecimal kcal) {
        return kcal.divide(BigDecimal.TEN, 0, RoundingMode.HALF_UP).multiply(BigDecimal.TEN).intValueExact();
    }
}
