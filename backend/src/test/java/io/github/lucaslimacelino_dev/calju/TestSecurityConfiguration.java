package io.github.lucaslimacelino_dev.calju;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.DynamicPropertyRegistrar;

import com.nimbusds.jose.jwk.source.ImmutableSecret;

/**
 * Segredo dos tokens só para os testes: gerado aleatoriamente a cada execução, nunca gravado.
 * {@link #token} assina tokens de verdade com ele, como o login da Fase 6 fará.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestSecurityConfiguration {

    private static final byte[] KEY = randomKey();
    private static final SecretKey SECRET = new SecretKeySpec(KEY, "HmacSHA256");

    private static byte[] randomKey() {
        byte[] key = new byte[32];
        new SecureRandom().nextBytes(key);
        return key;
    }

    @Bean
    DynamicPropertyRegistrar jwtSecret() {
        return registry -> registry.add("calju.security.jwt-secret", () -> Base64.getEncoder().encodeToString(KEY));
    }

    public static String token(UUID subject, String issuer, Instant expiresAt) {
        return token(subject.toString(), issuer, expiresAt, SECRET);
    }

    public static String token(String subject, String issuer, Instant expiresAt, SecretKey key) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuer(issuer)
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .build();
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
