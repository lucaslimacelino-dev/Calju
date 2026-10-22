package io.github.lucaslimacelino_dev.calju;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;

import io.github.lucaslimacelino_dev.calju.auth.GoogleIdTokenDecoder;
import io.github.lucaslimacelino_dev.calju.auth.TestGoogleValidators;

/**
 * "Google de mentira" para os testes: uma chave RSA gerada na hora assina ID tokens no formato do
 * Google, e o decoder de teste aplica as mesmas regras (emissor, público, validade) do de verdade.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestGoogleConfiguration {

    public static final String CLIENT_ID = "cliente-web-de-teste.apps.googleusercontent.com";
    private static final RSAKey KEY = generate();

    private static RSAKey generate() {
        try {
            return new RSAKeyGenerator(2048).keyID(UUID.randomUUID().toString()).generate();
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
    }

    @Bean
    @Primary
    GoogleIdTokenDecoder testGoogleDecoder() throws JOSEException {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
        decoder.setJwtValidator(TestGoogleValidators.of(CLIENT_ID));
        return new GoogleIdTokenDecoder() {
            @Override
            public boolean isEnabled() {
                return true;
            }

            @Override
            public Jwt decode(String idToken) {
                return decoder.decode(idToken);
            }
        };
    }

    /** ID token como o Google emitiria; {@code overrides} troca qualquer claim (null remove). */
    public static String idToken(String subject, String email, Map<String, Object> overrides) {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("https://accounts.google.com")
                .audience(List.of(CLIENT_ID))
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("email", email)
                .claim("email_verified", true);
        overrides.forEach((name, value) -> {
            if (value == null) {
                claims.claims(all -> all.remove(name));
            } else {
                claims.claim(name, value);
            }
        });
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(KEY)));
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).keyId(KEY.getKeyID()).build(), claims.build())).getTokenValue();
    }
}
