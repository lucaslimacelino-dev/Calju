package io.github.lucaslimacelino_dev.calju.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Login. Sem @Email aqui: um formato inválido recebe o mesmo 401 genérico de uma senha errada. */
public record LoginRequest(
        @NotBlank @Size(max = 254) String email,
        @NotBlank @Size(max = 128) String password,
        @Size(max = 100) String deviceLabel) {

    public LoginRequest {
        email = email == null ? null : email.trim();
    }
}
