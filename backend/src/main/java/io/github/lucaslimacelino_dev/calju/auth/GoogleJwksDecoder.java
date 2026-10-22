package io.github.lucaslimacelino_dev.calju.auth;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * ID tokens do Google conferidos como manda a documentação do Google: assinatura com as chaves
 * públicas publicadas por ele, {@code iss} = accounts.google.com, {@code aud} = nosso client ID
 * (tipo Web) e {@code exp} no futuro.
 */
@Component
public class GoogleJwksDecoder implements GoogleIdTokenDecoder {

    static final String JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";
    private static final Set<String> ISSUERS = Set.of("accounts.google.com", "https://accounts.google.com");

    private final String clientId;
    private final NimbusJwtDecoder decoder;

    public GoogleJwksDecoder(@Value("${calju.google.client-id}") String clientId) {
        this.clientId = clientId == null ? "" : clientId.trim();
        this.decoder = NimbusJwtDecoder.withJwkSetUri(JWKS_URI).build();
        this.decoder.setJwtValidator(validator(this.clientId));
    }

    /** As mesmas regras para o decoder de verdade e para o dos testes. */
    static OAuth2TokenValidator<Jwt> validator(String clientId) {
        OAuth2TokenValidator<Jwt> issuer = jwt -> ISSUERS.contains(jwt.getClaimAsString("iss"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Emissor inválido", null));
        OAuth2TokenValidator<Jwt> audience = jwt -> {
            List<String> aud = jwt.getAudience();
            return aud != null && aud.contains(clientId)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Público inválido", null));
        };
        return new DelegatingOAuth2TokenValidator<>(new JwtTimestampValidator(), issuer, audience);
    }

    @Override
    public boolean isEnabled() {
        return !clientId.isEmpty();
    }

    @Override
    public Jwt decode(String idToken) throws JwtException {
        return decoder.decode(idToken);
    }
}
