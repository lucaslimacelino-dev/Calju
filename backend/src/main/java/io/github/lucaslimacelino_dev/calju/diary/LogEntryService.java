package io.github.lucaslimacelino_dev.calju.diary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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
import io.github.lucaslimacelino_dev.calju.common.NutrientValidator;
import io.github.lucaslimacelino_dev.calju.common.Saved;
import io.github.lucaslimacelino_dev.calju.common.Versions;
import io.github.lucaslimacelino_dev.calju.diary.dto.LogEntryRequest;
import io.github.lucaslimacelino_dev.calju.diary.dto.LogEntryResponse;
import io.github.lucaslimacelino_dev.calju.diary.dto.LogItemDto;
import io.github.lucaslimacelino_dev.calju.food.dto.NutrientValueDto;
import io.github.lucaslimacelino_dev.calju.user.UserRepository;

/**
 * Diário. O servidor guarda o snapshot que o app enviou; ele não recalcula nutrientes nem lê o
 * catálogo (o catálogo oficial fica só no app).
 */
@Service
public class LogEntryService {

    private static final String NOT_FOUND = "Registro não encontrado.";
    /** Limite de uma consulta por período, para uma requisição não varrer anos de diário. */
    static final int MAX_RANGE_DAYS = 366;

    private final LogEntryRepository entries;
    private final NutrientValidator nutrients;
    private final UserRepository users;
    private final Clock clock;

    public LogEntryService(UserRepository users, LogEntryRepository entries, NutrientValidator nutrients, Clock clock) {
        this.users = users;
        this.entries = entries;
        this.nutrients = nutrients;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<LogEntryResponse> range(UUID ownerId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new BadRequestException("O fim do período vem antes do início.");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_RANGE_DAYS) {
            throw new BadRequestException("Período máximo: " + MAX_RANGE_DAYS + " dias.");
        }
        return entries
                .findByOwnerIdAndDeletedAtIsNullAndLogDateBetweenOrderByLogDateAscCreatedAtAsc(
                        ownerId, from, to)
                .stream().map(LogEntryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public LogEntryResponse get(UUID ownerId, UUID id) {
        return LogEntryResponse.from(entries.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new NotFoundException(NOT_FOUND)));
    }

    /**
     * Cria ou atualiza. Itens com id já existente são atualizados no lugar; os que não vierem
     * saem; os novos entram. Um PUT num registro excluído o restaura (desfazer).
     */
    @Transactional
    public Saved<LogEntryResponse> save(UUID ownerId, UUID id, LogEntryRequest request) {
        users.lockForWrite(ownerId);
        validateItems(request.items());
        Optional<LogEntryEntity> existing = entries.findForUpdate(id);
        boolean created = existing.isEmpty();
        LogEntryEntity entry;
        if (created) {
            if (request.version() != Versions.NEW) {
                throw new NotFoundException(NOT_FOUND);
            }
            entry = new LogEntryEntity(id, ownerId);
        } else {
            entry = owned(existing.get(), ownerId);
            Versions.requireCurrent(request.version(), entry.getVersion());
        }
        entry.apply(request.logDate(), request.occasion(), blankToNull(request.title()), request.sourceMealId(),
                request.createdAt(), clock.instant());
        List<UUID> itemIds = new ArrayList<>();
        for (int i = 0; i < request.items().size(); i++) {
            LogItemDto dto = request.items().get(i);
            itemIds.add(dto.id());
            entry.itemFor(dto.id()).apply(dto.foodId(), dto.foodName().trim(), blankToNull(dto.brand()),
                    blankToNull(dto.source()), dto.quantityG(), blankToNull(dto.portionLabel()), i,
                    dto.nutrients().stream().map(NutrientValueDto::toAmount).toList());
        }
        entry.keepOnly(itemIds);
        return new Saved<>(LogEntryResponse.from(entries.saveAndFlush(entry)), created);
    }

    @Transactional
    public LogEntryResponse delete(UUID ownerId, UUID id, int version) {
        users.lockForWrite(ownerId);
        LogEntryEntity entry = owned(entries.findForUpdate(id).orElseThrow(() -> new NotFoundException(NOT_FOUND)),
                ownerId);
        Versions.requireCurrent(version, entry.getVersion());
        entry.markDeleted(clock.instant());
        return LogEntryResponse.from(entries.saveAndFlush(entry));
    }

    private void validateItems(List<LogItemDto> items) {
        Set<UUID> ids = new HashSet<>();
        for (LogItemDto item : items) {
            if (!ids.add(item.id())) {
                throw new BadRequestException("Item repetido: " + item.id() + ".");
            }
            nutrients.requireValid(item.nutrients().stream().map(NutrientValueDto::code).toList());
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static LogEntryEntity owned(LogEntryEntity entry, UUID ownerId) {
        if (!entry.getOwnerId().equals(ownerId)) {
            throw new NotFoundException(NOT_FOUND);
        }
        return entry;
    }
}
