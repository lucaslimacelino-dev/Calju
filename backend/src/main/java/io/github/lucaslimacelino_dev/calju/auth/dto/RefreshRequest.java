package io.github.lucaslimacelino_dev.calju.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RefreshRequest(@NotBlank @Size(max = 100) String refreshToken) {
}
