package io.github.lucaslimacelino_dev.calju.auth;

/** Envio dos e-mails de conta. Os testes trocam a implementação por uma que só guarda as mensagens. */
public interface CodeMailer {

    void sendVerificationCode(String email, String code);

    void sendResetCode(String email, String code);

    /** Alguém tentou cadastrar um e-mail que já tem conta: avisa o dono, sem revelar nada a quem tentou. */
    void sendAlreadyRegistered(String email);
}
