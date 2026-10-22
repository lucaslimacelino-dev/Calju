package io.github.lucaslimacelino_dev.calju.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    /** Trava a linha: dois "renovar" simultâneos com o mesmo token não podem ambos funcionar. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
    int revokeFamily(UUID familyId, Instant now);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    int revokeAllOfUser(UUID userId, Instant now);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :now "
            + "where t.userId = :userId and t.familyId <> :familyId and t.revokedAt is null")
    int revokeAllOfUserExceptFamily(UUID userId, UUID familyId, Instant now);

    /** O token vivo de cada sessão (cada família tem no máximo um não revogado). */
    @Query("select t from RefreshTokenEntity t where t.userId = :userId and t.revokedAt is null "
            + "and t.expiresAt > :now order by t.createdAt desc")
    List<RefreshTokenEntity> findActive(UUID userId, Instant now);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revokedAt = :now "
            + "where t.userId = :userId and t.familyId = :familyId and t.revokedAt is null")
    int revokeFamilyOfUser(UUID userId, UUID familyId, Instant now);
}
