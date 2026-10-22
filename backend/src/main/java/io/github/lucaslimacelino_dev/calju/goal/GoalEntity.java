package io.github.lucaslimacelino_dev.calju.goal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Meta diária; vale a partir de {@code effectiveFrom} até a próxima meta do mesmo usuário. */
@Entity
@Table(name = "nutrition_goals")
public class GoalEntity {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false, precision = 12, scale = 4)
    private BigDecimal kcal;

    @Column(name = "protein_g", precision = 12, scale = 4)
    private BigDecimal proteinG;

    @Column(name = "carbs_g", precision = 12, scale = 4)
    private BigDecimal carbohydrateG;

    @Column(name = "fat_g", precision = 12, scale = 4)
    private BigDecimal fatG;

    @Column(nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected GoalEntity() {
    }

    public GoalEntity(UUID id, UUID ownerId) {
        this.id = id;
        this.ownerId = ownerId;
    }

    /** Aplica os novos valores e avança a versão (1 na criação). */
    void apply(LocalDate effectiveFrom, BigDecimal kcal, BigDecimal proteinG, BigDecimal carbohydrateG,
               BigDecimal fatG, Instant now) {
        this.effectiveFrom = effectiveFrom;
        this.kcal = kcal;
        this.proteinG = proteinG;
        this.carbohydrateG = carbohydrateG;
        this.fatG = fatG;
        this.version++;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public BigDecimal getKcal() {
        return kcal;
    }

    public BigDecimal getProteinG() {
        return proteinG;
    }

    public BigDecimal getCarbohydrateG() {
        return carbohydrateG;
    }

    public BigDecimal getFatG() {
        return fatG;
    }

    public int getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
