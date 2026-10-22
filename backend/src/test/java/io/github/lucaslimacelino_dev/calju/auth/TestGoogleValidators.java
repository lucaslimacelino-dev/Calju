package io.github.lucaslimacelino_dev.calju.auth;

import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;

/** Dá aos testes acesso às regras de validação de produção (que são do pacote auth). */
public final class TestGoogleValidators {

    private TestGoogleValidators() {
    }

    public static OAuth2TokenValidator<Jwt> of(String clientId) {
        return GoogleJwksDecoder.validator(clientId);
    }
}
