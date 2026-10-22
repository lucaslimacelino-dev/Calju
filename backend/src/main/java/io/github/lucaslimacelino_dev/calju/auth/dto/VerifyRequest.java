package io.github.lucaslimacelino_dev.calju.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Confirmação: e-mail, o código recebido e a senha escolhida (12 a 128 caracteres, sem regras de
 * composição, como recomenda o OWASP ASVS).
 */
public record VerifyRequest(
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "\\d{6}") String code,
        @NotBlank @Size(min = 12, max = 128) String password,
        @Size(max = 100) String deviceLabel) {

    public VerifyRequest {
        email = email == null ? null : email.trim();
    }
}
