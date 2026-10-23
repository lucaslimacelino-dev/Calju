package io.github.lucaslimacelino_dev.calju.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Confirmação da exclusão da conta pela página web: e-mail e o código recebido. */
public record DeletionConfirmRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "\\d{6}") String code) {

    public DeletionConfirmRequest {
        email = email == null ? null : email.trim();
    }
}
