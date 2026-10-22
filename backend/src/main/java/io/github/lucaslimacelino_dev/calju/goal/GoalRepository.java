package io.github.lucaslimacelino_dev.calju.goal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface GoalRepository extends JpaRepository<GoalEntity, UUID> {

    List<GoalEntity> findByOwnerIdOrderByEffectiveFrom(UUID ownerId);

    Optional<GoalEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    /** Trava a linha até o fim da transação: duas edições simultâneas não se atropelam. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select g from GoalEntity g where g.id = :id")
    Optional<GoalEntity> findForUpdate(UUID id);

    boolean existsByOwnerIdAndEffectiveFromAndIdNot(UUID ownerId, LocalDate effectiveFrom, UUID id);
}
