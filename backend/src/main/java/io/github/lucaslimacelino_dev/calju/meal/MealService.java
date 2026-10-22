package io.github.lucaslimacelino_dev.calju.meal;

import java.time.Clock;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lucaslimacelino_dev.calju.common.BadRequestException;
import io.github.lucaslimacelino_dev.calju.common.NotFoundException;
import io.github.lucaslimacelino_dev.calju.common.Saved;
import io.github.lucaslimacelino_dev.calju.common.Versions;
import io.github.lucaslimacelino_dev.calju.meal.dto.MealItemDto;
import io.github.lucaslimacelino_dev.calju.meal.dto.MealRequest;
import io.github.lucaslimacelino_dev.calju.meal.dto.MealResponse;
import io.github.lucaslimacelino_dev.calju.user.UserRepository;

/** Refeições salvas, com as mesmas regras de dono, versão e exclusão lógica dos alimentos. */
@Service
public class MealService {

    private static final String NOT_FOUND = "Refeição não encontrada.";

    private final MealRepository meals;
    private final UserRepository users;
    private final Clock clock;

    public MealService(UserRepository users, MealRepository meals, Clock clock) {
        this.users = users;
        this.meals = meals;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<MealResponse> list(UUID ownerId) {
        return meals.findByOwnerIdAndDeletedAtIsNullOrderByName(ownerId).stream().map(MealResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public MealResponse get(UUID ownerId, UUID id) {
        return MealResponse.from(meals.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND)));
    }

    @Transactional
    public Saved<MealResponse> save(UUID ownerId, UUID id, MealRequest request) {
        users.lockForWrite(ownerId);
        Optional<MealEntity> existing = meals.findForUpdate(id);
        boolean created = existing.isEmpty();
        MealEntity meal;
        if (created) {
            if (request.version() != Versions.NEW) {
                throw new NotFoundException(NOT_FOUND);
            }
            meal = new MealEntity(id, ownerId);
        } else {
            meal = owned(existing.get(), ownerId);
            Versions.requireCurrent(request.version(), meal.getVersion());
        }
        meal.apply(request.name().trim(), toItems(request.items()), clock.instant());
        return new Saved<>(MealResponse.from(meals.saveAndFlush(meal)), created);
    }

    @Transactional
    public MealResponse delete(UUID ownerId, UUID id, int version) {
        users.lockForWrite(ownerId);
        MealEntity meal = owned(meals.findForUpdate(id).orElseThrow(() -> new NotFoundException(NOT_FOUND)), ownerId);
        Versions.requireCurrent(version, meal.getVersion());
        meal.markDeleted(clock.instant());
        return MealResponse.from(meals.saveAndFlush(meal));
    }

    private static List<MealItem> toItems(List<MealItemDto> dtos) {
        Set<UUID> ids = new HashSet<>();
        List<MealItem> items = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            MealItemDto dto = dtos.get(i);
            if (!ids.add(dto.id())) {
                throw new BadRequestException("Item repetido: " + dto.id() + ".");
            }
            items.add(new MealItem(dto.id(), dto.foodId(), dto.quantityG(), i));
        }
        return items;
    }

    private static MealEntity owned(MealEntity meal, UUID ownerId) {
        if (!meal.getOwnerId().equals(ownerId)) {
            throw new NotFoundException(NOT_FOUND);
        }
        return meal;
    }
}
