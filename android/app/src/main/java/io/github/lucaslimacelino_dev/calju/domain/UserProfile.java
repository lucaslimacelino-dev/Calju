package io.github.lucaslimacelino_dev.calju.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Activity;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Objective;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Pace;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Sex;

/**
 * Respostas do questionário, guardadas no aparelho. Guarda o ano de nascimento (e não a idade)
 * para a idade avançar sozinha. {@code activity} é nula quando o questionário parou antes da
 * rotina (idade fora da faixa); {@code specialCondition}: gravidez, amamentação ou doença com
 * dieta orientada.
 */
public record UserProfile(Sex sex, int birthYear, BigDecimal heightCm, BigDecimal weightKg, Activity activity,
                          Objective objective, Pace pace, boolean specialCondition) {

    public UserProfile {
        Objects.requireNonNull(sex, "sex");
        Objects.requireNonNull(heightCm, "heightCm");
        Objects.requireNonNull(weightKg, "weightKg");
        Objects.requireNonNull(objective, "objective");
    }

    public static UserProfile fromAge(Sex sex, int age, LocalDate today, BigDecimal heightCm, BigDecimal weightKg,
                                      Activity activity, Objective objective, Pace pace, boolean specialCondition) {
        return new UserProfile(sex, today.getYear() - age, heightCm, weightKg, activity, objective, pace,
                specialCondition);
    }

    /** Idade aproximada (pode adiantar até 1 ano antes do aniversário). */
    public int age(LocalDate today) {
        return today.getYear() - birthYear;
    }
}
