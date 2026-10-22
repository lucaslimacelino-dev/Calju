package io.github.lucaslimacelino_dev.calju.diary;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** Lançamento do diário: um dia, uma ocasião e um ou mais itens. */
@Entity
@Table(name = "log_entries")
public class LogEntryEntity {

    @Id
    private UUID id;

    @Column(name = "owner_user_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Occasion occasion;

    @Column(length = 200)
    private String title;

    @Column(name = "source_meal_id")
    private UUID sourceMealId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Itens removidos da lista são apagados do banco (orphanRemoval). */
    @OneToMany(mappedBy = "entry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LogItemEntity> items = new ArrayList<>();

    @Column(nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected LogEntryEntity() {
    }

    public LogEntryEntity(UUID id, UUID ownerId) {
        this.id = id;
        this.ownerId = ownerId;
    }

    void apply(LocalDate logDate, Occasion occasion, String title, UUID sourceMealId, Instant createdAt,
               Instant now) {
        this.logDate = logDate;
        this.occasion = occasion;
        this.title = title;
        this.sourceMealId = sourceMealId;
        this.createdAt = createdAt;
        this.deletedAt = null;
        touch(now);
    }

    /** Item com este id, já existente neste lançamento; senão cria um novo. */
    LogItemEntity itemFor(UUID itemId) {
        for (LogItemEntity item : items) {
            if (item.getId().equals(itemId)) {
                return item;
            }
        }
        LogItemEntity item = new LogItemEntity(itemId, this);
        items.add(item);
        return item;
    }

    void keepOnly(List<UUID> itemIds) {
        items.removeIf(item -> !itemIds.contains(item.getId()));
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

    public LocalDate getLogDate() {
        return logDate;
    }

    public Occasion getOccasion() {
        return occasion;
    }

    public String getTitle() {
        return title;
    }

    public UUID getSourceMealId() {
        return sourceMealId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<LogItemEntity> getItemsInOrder() {
        return items.stream().sorted(Comparator.comparingInt(LogItemEntity::getPosition)).toList();
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
