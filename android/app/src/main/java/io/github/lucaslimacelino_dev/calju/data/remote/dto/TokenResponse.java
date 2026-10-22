package io.github.lucaslimacelino_dev.calju.data.remote.dto;

/** Par de tokens devolvido por verify, login, refresh e redefinição de senha. */
public record TokenResponse(String accessToken, String tokenType, long expiresIn, String refreshToken,
                            long refreshExpiresIn) {
}
