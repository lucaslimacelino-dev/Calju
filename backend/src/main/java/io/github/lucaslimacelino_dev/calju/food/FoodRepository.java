package io.github.lucaslimacelino_dev.calju.food;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface FoodRepository extends JpaRepository<FoodEntity, UUID> {

    List<FoodEntity> findByOwnerIdAndDeletedAtIsNullOrderByName(UUID ownerId);

    Optional<FoodEntity> findByIdAndOwnerId(UUID id, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FoodEntity f where f.id = :id")
    Optional<FoodEntity> findForUpdate(UUID id);
}
