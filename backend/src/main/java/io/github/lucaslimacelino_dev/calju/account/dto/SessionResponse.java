package io.github.lucaslimacelino_dev.calju.account.dto;

import java.time.Instant;
import java.util.UUID;

/** Um aparelho conectado. {@code current} = a sessão que fez esta requisição. */
public record SessionResponse(UUID id, String deviceLabel, Instant lastActiveAt, Instant expiresAt,
                              boolean current) {
}
