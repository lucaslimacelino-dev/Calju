package io.github.lucaslimacelino_dev.calju.common;

/** Token válido, mas de uma conta que não existe (ex.: excluída): tratado como não autenticado. */
public class AccountNotFoundException extends RuntimeException {

    public AccountNotFoundException() {
        super("Conta não encontrada.");
    }
}
