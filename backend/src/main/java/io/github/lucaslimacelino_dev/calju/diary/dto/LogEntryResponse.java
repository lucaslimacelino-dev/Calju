package io.github.lucaslimacelino_dev.calju.diary.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import io.github.lucaslimacelino_dev.calju.diary.LogEntryEntity;
import io.github.lucaslimacelino_dev.calju.diary.Occasion;

public record LogEntryResponse(UUID id, LocalDate logDate, Occasion occasion, String title, UUID sourceMealId,
                               Instant createdAt, List<LogItemDto> items, int version, Instant updatedAt,
                               Instant deletedAt) {

    public static LogEntryResponse from(LogEntryEntity entry) {
        return new LogEntryResponse(entry.getId(), entry.getLogDate(), entry.getOccasion(), entry.getTitle(),
                entry.getSourceMealId(), entry.getCreatedAt(),
                entry.getItemsInOrder().stream().map(LogItemDto::from).toList(),
                entry.getVersion(), entry.getUpdatedAt(), entry.getDeletedAt());
    }
}
