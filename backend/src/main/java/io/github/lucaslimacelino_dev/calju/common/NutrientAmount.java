package io.github.lucaslimacelino_dev.calju.common;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/**
 * Um nutriente por 100 g, guardado como linha (código INFOODS). Usado pelos alimentos do usuário
 * e, na etapa do diário, pela cópia dos nutrientes no registro.
 */
@Embeddable
public class NutrientAmount {

    @Column(name = "nutrient_code", nullable = false, length = 16)
    private String code;

    @Column(name = "amount_per_100g", precision = 12, scale = 4)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 5)
    private Qualifier qualifier;

    protected NutrientAmount() {
    }

    public NutrientAmount(String code, BigDecimal amount, Qualifier qualifier) {
        this.code = code;
        this.amount = amount;
        this.qualifier = qualifier;
    }

    public String getCode() {
        return code;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public Qualifier getQualifier() {
        return qualifier;
    }
}
