package io.github.lucaslimacelino_dev.calju.diary.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import io.github.lucaslimacelino_dev.calju.diary.Occasion;

/**
 * Corpo do PUT de um lançamento. {@code createdAt} é o horário do registro no aparelho (ordena
 * os lançamentos do dia). Lista completa de itens: o que não vier é removido.
 */
public record LogEntryRequest(
        @NotNull LocalDate logDate,
        @NotNull Occasion occasion,
        @Size(max = 200) String title,
        UUID sourceMealId,
        @NotNull Instant createdAt,
        @NotNull @Size(min = 1, max = 50) List<@Valid @NotNull LogItemDto> items,
        @NotNull @Min(0) Integer version) {
}
