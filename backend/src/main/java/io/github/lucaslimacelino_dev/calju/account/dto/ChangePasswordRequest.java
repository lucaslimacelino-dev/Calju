package io.github.lucaslimacelino_dev.calju.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Trocar a senha pede a atual: quem pegou o celular desbloqueado não troca a senha sozinho. */
public record ChangePasswordRequest(
        @NotBlank @Size(max = 128) String currentPassword,
        @NotBlank @Size(min = 12, max = 128) String newPassword) {
}
