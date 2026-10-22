package io.github.lucaslimacelino_dev.calju.data.remote.dto;

/** Corpos das rotas de conta (/api/v1/auth), com os mesmos nomes de campo do backend. */
public final class AuthBodies {

    private AuthBodies() {
    }

    public record Email(String email) {
    }

    public record CodeAndPassword(String email, String code, String password, String deviceLabel) {
    }

    public record Login(String email, String password, String deviceLabel) {
    }

    public record Refresh(String refreshToken) {
    }
}
