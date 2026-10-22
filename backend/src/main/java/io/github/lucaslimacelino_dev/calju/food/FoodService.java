package io.github.lucaslimacelino_dev.calju.food;

import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.lucaslimacelino_dev.calju.common.NotFoundException;
import io.github.lucaslimacelino_dev.calju.common.NutrientValidator;
import io.github.lucaslimacelino_dev.calju.common.Saved;
import io.github.lucaslimacelino_dev.calju.common.Versions;
import io.github.lucaslimacelino_dev.calju.food.dto.FoodRequest;
import io.github.lucaslimacelino_dev.calju.food.dto.FoodResponse;
import io.github.lucaslimacelino_dev.calju.food.dto.NutrientValueDto;

/**
 * Alimentos do usuário. Exclusão lógica: o registro fica (com deletedAt) para a sincronização.
 * Devolve DTOs já montados: nutrientes e porção são lidos do banco sob demanda, dentro da transação.
 */
@Service
public class FoodService {

    private static final String NOT_FOUND = "Alimento não encontrado.";

    private final FoodRepository foods;
    private final NutrientValidator nutrients;
    private final Clock clock;

    public FoodService(FoodRepository foods, NutrientValidator nutrients, Clock clock) {
        this.foods = foods;
        this.nutrients = nutrients;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<FoodResponse> list(UUID ownerId) {
        return foods.findByOwnerIdAndDeletedAtIsNullOrderByName(ownerId).stream().map(FoodResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FoodResponse get(UUID ownerId, UUID id) {
        return FoodResponse.from(foods.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND)));
    }

    /** Cria ou substitui o alimento; um PUT num alimento excluído o restaura (desfazer). */
    @Transactional
    public Saved<FoodResponse> save(UUID ownerId, UUID id, FoodRequest request) {
        nutrients.requireValid(request.nutrients().stream().map(NutrientValueDto::code).toList());
        Optional<FoodEntity> existing = foods.findForUpdate(id);
        boolean created = existing.isEmpty();
        FoodEntity food;
        if (created) {
            if (request.version() != Versions.NEW) {
                throw new NotFoundException(NOT_FOUND);
            }
            food = new FoodEntity(id, ownerId);
        } else {
            food = owned(existing.get(), ownerId);
            Versions.requireCurrent(request.version(), food.getVersion());
        }
        String brand = request.brand() == null || request.brand().isBlank() ? null : request.brand().trim();
        FoodPortion portion = request.portion() == null ? null
                : new FoodPortion(request.portion().label().trim(), request.portion().grams());
        food.apply(request.name().trim(), brand,
                request.nutrients().stream().map(NutrientValueDto::toAmount).toList(), portion, clock.instant());
        return new Saved<>(FoodResponse.from(foods.saveAndFlush(food)), created);
    }

    @Transactional
    public FoodResponse delete(UUID ownerId, UUID id, int version) {
        FoodEntity food = owned(foods.findForUpdate(id).orElseThrow(() -> new NotFoundException(NOT_FOUND)), ownerId);
        Versions.requireCurrent(version, food.getVersion());
        food.markDeleted(clock.instant());
        return FoodResponse.from(foods.saveAndFlush(food));
    }

    private static FoodEntity owned(FoodEntity food, UUID ownerId) {
        if (!food.getOwnerId().equals(ownerId)) {
            throw new NotFoundException(NOT_FOUND);
        }
        return food;
    }
}
