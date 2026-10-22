package io.github.lucaslimacelino_dev.calju.account.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;

/**
 * Excluir a conta pede uma confirmação: a senha, ou (contas que entram pelo Google) um ID token
 * novo do Google da mesma conta vinculada. Exatamente um dos dois.
 */
public record DeleteAccountRequest(
        @Size(max = 128) String password,
        @Size(max = 4096) String googleIdToken) {

    @AssertTrue(message = "Informe a senha ou a confirmação do Google")
    public boolean isOneConfirmation() {
        return (password != null && !password.isBlank()) != (googleIdToken != null && !googleIdToken.isBlank());
    }
}
