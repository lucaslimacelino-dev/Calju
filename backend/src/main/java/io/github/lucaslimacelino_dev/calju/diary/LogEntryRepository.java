package io.github.lucaslimacelino_dev.calju.diary;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface LogEntryRepository extends JpaRepository<LogEntryEntity, UUID> {

    List<LogEntryEntity> findByOwnerIdAndDeletedAtIsNullAndLogDateBetweenOrderByLogDateAscCreatedAtAsc(
            UUID ownerId, LocalDate from, LocalDate to);

    Optional<LogEntryEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from LogEntryEntity e where e.id = :id")
    Optional<LogEntryEntity> findForUpdate(UUID id);
}
