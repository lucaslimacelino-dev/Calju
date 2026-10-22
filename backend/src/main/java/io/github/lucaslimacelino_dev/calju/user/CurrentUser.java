package io.github.lucaslimacelino_dev.calju.user;

import java.util.UUID;

/**
 * O usuário da requisição, tirado do token (claim "sub"), e a sessão de onde veio (claim "sid";
 * nula em tokens sem sessão). Controllers o recebem como parâmetro; o cliente nunca informa o
 * próprio id.
 */
public record CurrentUser(UUID id, UUID sessionId) {
}
