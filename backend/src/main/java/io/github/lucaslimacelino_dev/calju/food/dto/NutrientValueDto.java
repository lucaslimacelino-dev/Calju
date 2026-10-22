package io.github.lucaslimacelino_dev.calju.food.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import io.github.lucaslimacelino_dev.calju.common.NutrientAmount;
import io.github.lucaslimacelino_dev.calju.common.Qualifier;

/** Um nutriente por 100 g: VALUE exige quantidade (zero vale); TRACE não tem quantidade. */
public record NutrientValueDto(
        @NotNull @Pattern(regexp = "[A-Z0-9]{1,16}") String code,
        @DecimalMin("0") @Digits(integer = 8, fraction = 4) BigDecimal amount,
        @NotNull Qualifier qualifier) {

    @AssertTrue(message = "VALUE exige quantidade; TRACE não aceita quantidade")
    public boolean isAmountConsistent() {
        return qualifier == null || (qualifier == Qualifier.VALUE) == (amount != null);
    }

    public NutrientAmount toAmount() {
        return new NutrientAmount(code, amount, qualifier);
    }

    public static NutrientValueDto from(NutrientAmount amount) {
        return new NutrientValueDto(amount.getCode(), amount.getAmount(), amount.getQualifier());
    }
}
