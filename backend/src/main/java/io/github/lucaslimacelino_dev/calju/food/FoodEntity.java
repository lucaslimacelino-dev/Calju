package io.github.lucaslimacelino_dev.calju.food;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import io.github.lucaslimacelino_dev.calju.common.NutrientAmount;

/**
 * Alimento criado pelo usuário. Nutrientes e porção são coleções do próprio alimento: gravar o
 * alimento grava as linhas de food_nutrients e food_portions junto.
 */
@Entity
@Table(name = "foods")
public class FoodEntity {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 200)
    private String brand;

    @Column(nullable = false, length = 2)
    private String unit = "g";

    @ElementCollection
    @CollectionTable(name = "food_nutrients", joinColumns = @JoinColumn(name = "food_id"))
    private List<NutrientAmount> nutrients = new ArrayList<>();

    /** Zero ou um elemento (a tabela tem food_id como chave). */
    @ElementCollection
    @CollectionTable(name = "food_portions", joinColumns = @JoinColumn(name = "food_id"))
    private List<FoodPortion> portions = new ArrayList<>();

    @Column(nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected FoodEntity() {
    }

    public FoodEntity(UUID id, UUID ownerId) {
        this.id = id;
        this.ownerId = ownerId;
    }

    /** Substitui tudo (nutriente que não veio passa a ser desconhecido) e reativa se estava excluído. */
    void apply(String name, String brand, String unit, List<NutrientAmount> nutrients, FoodPortion portion,
               Instant now) {
        this.name = name;
        this.brand = brand;
        this.unit = unit;
        this.nutrients.clear();
        this.nutrients.addAll(nutrients);
        this.portions.clear();
        if (portion != null) {
            this.portions.add(portion);
        }
        this.deletedAt = null;
        touch(now);
    }

    void markDeleted(Instant now) {
        this.deletedAt = now;
        touch(now);
    }

    private void touch(Instant now) {
        this.version++;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    /** "g" ou "ml"; os valores são por 100 da unidade. */
    public String getUnit() {
        return unit;
    }

    public List<NutrientAmount> getNutrients() {
        return nutrients;
    }

    public FoodPortion getPortion() {
        return portions.isEmpty() ? null : portions.get(0);
    }

    public int getVersion() {
        return version;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }
}
