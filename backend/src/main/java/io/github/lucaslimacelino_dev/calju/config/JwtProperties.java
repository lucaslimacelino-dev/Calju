package io.github.lucaslimacelino_dev.calju.config;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import jakarta.validation.constraints.NotBlank;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuração dos tokens de acesso. O segredo vem de CALJU_JWT_SECRET (Base64); sem ele, ou
 * com menos de 256 bits, o backend não inicia.
 */
@Validated
@ConfigurationProperties("calju.security")
public record JwtProperties(@NotBlank String jwtSecret, @NotBlank String jwtIssuer) {

    private static final int MIN_KEY_BYTES = 32;

    public SecretKey secretKey() {
        byte[] key;
        try {
            key = Base64.getDecoder().decode(jwtSecret.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("CALJU_JWT_SECRET não é Base64 válido", e);
        }
        if (key.length < MIN_KEY_BYTES) {
            throw new IllegalStateException("CALJU_JWT_SECRET precisa ter pelo menos 32 bytes (256 bits)");
        }
        return new SecretKeySpec(key, "HmacSHA256");
    }
}
