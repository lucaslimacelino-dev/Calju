package io.github.lucaslimacelino_dev.calju.common;

/** 429: limite de tentativas atingido. {@code retryAfterSeconds} vai no cabeçalho Retry-After. */
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(long retryAfterSeconds) {
        super("Muitas tentativas. Tente de novo em alguns minutos.");
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long retryAfterSeconds() {
        return retryAfterSeconds;
    }
}
