package io.github.lucaslimacelino_dev.calju.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** O ID token que o app recebeu do Google (Credential Manager). */
public record GoogleLoginRequest(
        @NotBlank @Size(max = 4096) String idToken,
        @Size(max = 100) String deviceLabel) {
}
