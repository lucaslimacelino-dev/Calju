package io.github.lucaslimacelino_dev.calju.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface EmailCodeRepository extends JpaRepository<EmailCodeEntity, UUID> {

    /** O código mais recente ainda não usado (os anteriores são invalidados ao gerar um novo). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EmailCodeEntity> findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(
            UUID userId, CodePurpose purpose);

    @Modifying
    @Query("update EmailCodeEntity c set c.usedAt = :now "
            + "where c.userId = :userId and c.purpose = :purpose and c.usedAt is null")
    void invalidateAll(UUID userId, CodePurpose purpose, Instant now);
}
