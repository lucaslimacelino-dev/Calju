package io.github.lucaslimacelino_dev.calju.common;

/** Recurso inexistente ou de outro usuário: as duas situações respondem 404 (não revelam posse). */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
