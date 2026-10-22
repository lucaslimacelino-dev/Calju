package io.github.lucaslimacelino_dev.calju.common;

/** 401 de login ou renovação. A mensagem é sempre genérica: não diz qual parte estava errada. */
public class AuthFailedException extends RuntimeException {

    public AuthFailedException(String message) {
        super(message);
    }
}
