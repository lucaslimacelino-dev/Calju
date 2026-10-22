package io.github.lucaslimacelino_dev.calju.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Um refresh token (só o hash). {@code familyId} = a sessão aberta num login. */
@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Column(name = "family_id", nullable = false, updatable = false)
    private UUID familyId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "device_label", length = 100)
    private String deviceLabel;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private UUID replacedById;

    protected RefreshTokenEntity() {
    }

    RefreshTokenEntity(UUID userId, UUID familyId, String tokenHash, String deviceLabel, Instant now,
                       Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.familyId = familyId;
        this.tokenHash = tokenHash;
        this.deviceLabel = deviceLabel;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    boolean isRevoked() {
        return revokedAt != null;
    }

    boolean isExpired(Instant now) {
        return !now.isBefore(expiresAt);
    }

    void replaceWith(UUID newId, Instant now) {
        this.revokedAt = now;
        this.replacedById = newId;
    }

    UUID getId() {
        return id;
    }

    UUID getUserId() {
        return userId;
    }

    UUID getFamilyId() {
        return familyId;
    }

    String getDeviceLabel() {
        return deviceLabel;
    }
}
