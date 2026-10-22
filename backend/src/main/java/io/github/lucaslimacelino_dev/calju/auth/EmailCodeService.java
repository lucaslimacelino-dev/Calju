package io.github.lucaslimacelino_dev.calju.auth;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Códigos de 6 dígitos. Cada novo código invalida os anteriores do mesmo tipo; vale 30 minutos e
 * aceita 5 tentativas erradas. O código só existe em texto no e-mail; o banco guarda o hash.
 */
@Service
public class EmailCodeService {

    static final Duration VALIDITY = Duration.ofMinutes(30);
    static final int MAX_ATTEMPTS = 5;

    private final EmailCodeRepository codes;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public EmailCodeService(EmailCodeRepository codes, Clock clock) {
        this.codes = codes;
        this.clock = clock;
    }

    /** Gera e guarda um código novo; devolve o código em texto para ir no e-mail. */
    @Transactional
    public String issue(UUID userId, CodePurpose purpose) {
        Instant now = clock.instant();
        codes.invalidateAll(userId, purpose, now);
        String code = "%06d".formatted(random.nextInt(1_000_000));
        codes.save(new EmailCodeEntity(userId, purpose, Hashes.sha256(code), now, now.plus(VALIDITY)));
        return code;
    }

    /**
     * Confere o código. Acerto: o código é marcado como usado. Erro: conta uma tentativa (e o
     * chamador deve deixar a transação gravar isso, mesmo respondendo erro).
     */
    @Transactional
    public boolean consume(UUID userId, CodePurpose purpose, String code) {
        Instant now = clock.instant();
        Optional<EmailCodeEntity> latest =
                codes.findFirstByUserIdAndPurposeAndUsedAtIsNullOrderByCreatedAtDesc(userId, purpose);
        if (latest.isEmpty() || !latest.get().isUsable(now, MAX_ATTEMPTS)) {
            return false;
        }
        EmailCodeEntity stored = latest.get();
        if (Hashes.sameHash(stored.getCodeHash(), Hashes.sha256(code))) {
            stored.markUsed(now);
            return true;
        }
        stored.countFailedAttempt();
        return false;
    }
}
