package io.github.lucaslimacelino_dev.calju.auth;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Confere um ID token do Google (assinatura, emissor, público e validade). Uma interface própria,
 * e não um JwtDecoder do Spring, para não se confundir com o decoder dos tokens do CaLju; os testes
 * trocam a implementação por uma com chave local.
 */
public interface GoogleIdTokenDecoder {

    boolean isEnabled();

    Jwt decode(String idToken) throws JwtException;
}
