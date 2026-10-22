package io.github.lucaslimacelino_dev.calju.auth.dto;

/** Par de tokens: o de acesso (curto, vai em cada requisição) e o de renovação (guardado no aparelho). */
public record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken,
                            long refreshExpiresIn) {
}
