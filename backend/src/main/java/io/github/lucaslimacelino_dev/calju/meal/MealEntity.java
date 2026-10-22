package io.github.lucaslimacelino_dev.calju.meal;

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
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Refeição salva (molde). Os itens apontam para o alimento atual, não para uma cópia. */
@Entity
@Table(name = "meals")
public class MealEntity {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(nullable = false, length = 200)
    private String name;

    @ElementCollection
    @CollectionTable(name = "meal_items", joinColumns = @JoinColumn(name = "meal_id"))
    @OrderBy("position")
    private List<MealItem> items = new ArrayList<>();

    @Column(nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected MealEntity() {
    }

    public MealEntity(UUID id, UUID ownerId) {
        this.id = id;
        this.ownerId = ownerId;
    }

    void apply(String name, List<MealItem> items, Instant now) {
        this.name = name;
        this.items.clear();
        this.items.addAll(items);
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

    public List<MealItem> getItems() {
        return items;
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
