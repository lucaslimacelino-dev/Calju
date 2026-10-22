package io.github.lucaslimacelino_dev.calju.data.remote.dto;

/** Erro no formato ProblemDetail (RFC 9457): o app mostra {@code detail} ao usuário. */
public record ProblemResponse(String title, int status, String detail) {
}
