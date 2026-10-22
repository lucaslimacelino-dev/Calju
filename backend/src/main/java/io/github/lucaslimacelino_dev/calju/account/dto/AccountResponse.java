package io.github.lucaslimacelino_dev.calju.account.dto;

import java.util.UUID;

/** Dados da conta. {@code hasPassword} falso = conta que só entra pelo Google. */
public record AccountResponse(UUID id, String email, boolean hasPassword) {
}
