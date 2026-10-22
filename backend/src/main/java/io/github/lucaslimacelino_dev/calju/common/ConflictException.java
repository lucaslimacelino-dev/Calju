package io.github.lucaslimacelino_dev.calju.common;

/**
 * 409: a mudança não pode ser aplicada. {@code currentVersion} vem preenchido quando o cliente
 * enviou uma versão desatualizada, para que ele saiba qual é a atual.
 */
public class ConflictException extends RuntimeException {

    private final Integer currentVersion;

    public ConflictException(String message) {
        this(message, null);
    }

    public ConflictException(String message, Integer currentVersion) {
        super(message);
        this.currentVersion = currentVersion;
    }

    public Integer currentVersion() {
        return currentVersion;
    }
}
