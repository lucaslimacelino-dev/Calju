package io.github.lucaslimacelino_dev.calju.common;

/** 400 por regra de negócio que a validação dos campos não cobre (ex.: código de nutriente desconhecido). */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
