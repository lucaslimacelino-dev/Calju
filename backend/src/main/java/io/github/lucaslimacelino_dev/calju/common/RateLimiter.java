package io.github.lucaslimacelino_dev.calju.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Contador de tentativas por janela de tempo fixa, em memória (um servidor só, como no MVP).
 * Cada chave já inclui o nome da regra, então cada regra tem sua própria janela.
 */
@Component
public class RateLimiter {

    private static final int CLEANUP_THRESHOLD = 10_000;

    private record Window(Instant start, Duration period, int count) {

        boolean isOver(Instant now) {
            return !now.isBefore(start.plus(period));
        }
    }

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Conta uma tentativa; passou de {@code limit} dentro de {@code period}: 429. */
    public void check(String key, int limit, Duration period) {
        Instant now = clock.instant();
        if (windows.size() > CLEANUP_THRESHOLD) {
            windows.values().removeIf(window -> window.isOver(now));
        }
        Window window = windows.compute(key, (k, old) -> old == null || old.isOver(now)
                ? new Window(now, period, 1)
                : new Window(old.start(), old.period(), old.count() + 1));
        if (window.count() > limit) {
            long seconds = Duration.between(now, window.start().plus(period)).toSeconds();
            throw new TooManyRequestsException(Math.max(1, seconds));
        }
    }

    /** Zera os contadores. Usado pelos testes, para um teste não herdar tentativas de outro. */
    public void clear() {
        windows.clear();
    }
}
