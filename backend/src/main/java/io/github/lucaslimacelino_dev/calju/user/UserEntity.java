package io.github.lucaslimacelino_dev.calju.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Nulo em contas que só entram pelo Google (Fase 6, etapa 4) ou ainda não confirmadas. */
    @Column(name = "password_hash", length = 200)
    private String passwordHash;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    protected UserEntity() {
    }

    public UserEntity(UUID id, String email, Instant now) {
        this.id = id;
        this.email = email;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    /** Código conferido: a senha passa a valer e o e-mail fica confirmado. */
    public void confirmWithPassword(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        if (emailVerifiedAt == null) {
            this.emailVerifiedAt = now;
        }
        this.updatedAt = now;
    }

    public void changePasswordHash(String passwordHash, Instant now) {
        this.passwordHash = passwordHash;
        this.updatedAt = now;
    }
}
