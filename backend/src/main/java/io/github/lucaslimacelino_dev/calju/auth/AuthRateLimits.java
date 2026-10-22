package io.github.lucaslimacelino_dev.calju.auth;

import java.time.Duration;

import org.springframework.stereotype.Component;

import io.github.lucaslimacelino_dev.calju.common.RateLimiter;
import io.github.lucaslimacelino_dev.calju.user.Emails;

/**
 * Limites das rotas de conta (valores iniciais, ajustáveis depois de observar o uso real).
 * Login e códigos contam por (IP, e-mail) e por IP, nunca só por e-mail: assim ninguém tranca
 * a conta de outra pessoa errando a senha dela. Os pedidos que enviam e-mail também têm teto por
 * e-mail, para o CaLju não ser usado para lotar a caixa de entrada de alguém.
 */
@Component
public class AuthRateLimits {

    private static final Duration HOUR = Duration.ofHours(1);
    private static final Duration QUARTER = Duration.ofMinutes(15);

    private final RateLimiter limiter;

    public AuthRateLimits(RateLimiter limiter) {
        this.limiter = limiter;
    }

    /** Cadastro e "esqueci a senha": cada um pode enviar um e-mail. */
    void emailRequest(String rule, String ip, String email) {
        limiter.check(rule + ":ip:" + ip, 20, HOUR);
        limiter.check(rule + ":email:" + Emails.normalize(email), 5, HOUR);
    }

    /** Conferir código (cadastro ou redefinição). Cada código já aceita só 5 erros. */
    void codeAttempt(String ip, String email) {
        limiter.check("code:ip:" + ip, 50, QUARTER);
        limiter.check("code:ip-email:" + ip + ":" + Emails.normalize(email), 10, QUARTER);
    }

    void login(String ip, String email) {
        limiter.check("login:ip:" + ip, 100, QUARTER);
        limiter.check("login:ip-email:" + ip + ":" + Emails.normalize(email), 10, QUARTER);
    }

    void refresh(String ip) {
        limiter.check("refresh:ip:" + ip, 300, QUARTER);
    }
}
