package io.github.lucaslimacelino_dev.calju.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cadastro: só o e-mail. A senha é definida junto com o código, por quem recebeu o e-mail. */
public record RegisterRequest(@NotBlank @Email @Size(max = 254) String email) {

    /** Espaços nas pontas saem antes da validação (o app pode mandar o que foi colado). */
    public RegisterRequest {
        email = email == null ? null : email.trim();
    }
}
