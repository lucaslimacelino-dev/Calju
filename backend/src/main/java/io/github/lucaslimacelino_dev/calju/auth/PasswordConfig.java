package io.github.lucaslimacelino_dev.calju.auth;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Senhas com Argon2id: salt de 16 bytes, hash de 32, paralelismo 1, 19 MiB (19456 KiB) de
 * memória e 2 iterações (mínimo recomendado pela OWASP). O hash é gravado com o prefixo
 * {argon2}, o que permite trocar os parâmetros no futuro sem invalidar senhas antigas.
 */
@Configuration
public class PasswordConfig {

    static final String ARGON2 = "argon2";

    @Bean
    PasswordEncoder passwordEncoder() {
        Argon2PasswordEncoder argon2 = new Argon2PasswordEncoder(16, 32, 1, 19456, 2);
        return new DelegatingPasswordEncoder(ARGON2, Map.of(ARGON2, argon2));
    }
}
