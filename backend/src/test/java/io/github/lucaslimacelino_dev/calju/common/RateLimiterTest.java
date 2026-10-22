package io.github.lucaslimacelino_dev.calju.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    /** Relógio que o teste adianta à mão. */
    private static final class ManualClock extends Clock {

        private Instant now = Instant.parse("2026-10-22T12:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private final ManualClock clock = new ManualClock();
    private final RateLimiter limiter = new RateLimiter(clock);

    @Test
    void ateOLimitePassaDepoisDa429ComTempoDeEspera() {
        for (int i = 0; i < 3; i++) {
            limiter.check("login:x", 3, Duration.ofMinutes(15));
        }
        clock.advance(Duration.ofMinutes(5));

        assertThatThrownBy(() -> limiter.check("login:x", 3, Duration.ofMinutes(15)))
                .isInstanceOfSatisfying(TooManyRequestsException.class,
                        e -> assertThat(e.retryAfterSeconds()).isEqualTo(600));
    }

    @Test
    void janelaNovaZeraAContagem() {
        for (int i = 0; i < 3; i++) {
            limiter.check("login:x", 3, Duration.ofMinutes(15));
        }
        clock.advance(Duration.ofMinutes(15));

        limiter.check("login:x", 3, Duration.ofMinutes(15));
    }

    @Test
    void chavesDiferentesNaoSeAfetam() {
        for (int i = 0; i < 3; i++) {
            limiter.check("login:a", 3, Duration.ofMinutes(15));
        }

        limiter.check("login:b", 3, Duration.ofMinutes(15));
    }
}
