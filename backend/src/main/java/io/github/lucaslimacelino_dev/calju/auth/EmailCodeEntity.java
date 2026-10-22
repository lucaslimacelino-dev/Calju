package io.github.lucaslimacelino_dev.calju.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "email_codes")
public class EmailCodeEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CodePurpose purpose;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(nullable = false)
    private int attempts;

    protected EmailCodeEntity() {
    }

    EmailCodeEntity(UUID userId, CodePurpose purpose, String codeHash, Instant now, Instant expiresAt) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.createdAt = now;
        this.expiresAt = expiresAt;
    }

    boolean isUsable(Instant now, int maxAttempts) {
        return usedAt == null && now.isBefore(expiresAt) && attempts < maxAttempts;
    }

    String getCodeHash() {
        return codeHash;
    }

    void markUsed(Instant now) {
        this.usedAt = now;
    }

    void countFailedAttempt() {
        this.attempts++;
    }
}
