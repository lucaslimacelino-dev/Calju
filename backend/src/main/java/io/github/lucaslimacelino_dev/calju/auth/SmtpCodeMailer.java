package io.github.lucaslimacelino_dev.calju.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Envia por SMTP (em desenvolvimento, para o Mailpit do docker-compose). Texto simples, sem links. */
@Component
public class SmtpCodeMailer implements CodeMailer {

    private final JavaMailSender sender;
    private final String from;

    public SmtpCodeMailer(JavaMailSender sender, @Value("${calju.mail.from}") String from) {
        this.sender = sender;
        this.from = from;
    }

    @Override
    public void sendVerificationCode(String email, String code) {
        send(email, "Seu código do CaLju: " + code, """
                Olá! Aqui é o Caju.

                Seu código para confirmar o e-mail no CaLju é: %s

                Ele vale por 30 minutos. Se não foi você que pediu, ignore este e-mail.
                """.formatted(code));
    }

    @Override
    public void sendResetCode(String email, String code) {
        send(email, "Redefinir a senha do CaLju: " + code, """
                Olá! Aqui é o Caju.

                Seu código para criar uma senha nova no CaLju é: %s

                Ele vale por 30 minutos. Ao trocar a senha, todos os aparelhos conectados saem da conta.
                Se não foi você que pediu, ignore este e-mail: sua senha continua a mesma.
                """.formatted(code));
    }

    @Override
    public void sendAlreadyRegistered(String email) {
        send(email, "Tentativa de cadastro no CaLju", """
                Olá! Aqui é o Caju.

                Alguém tentou criar uma conta no CaLju com este e-mail, mas você já tem uma.
                Se foi você, entre normalmente ou use "Esqueci a senha" no app.
                Se não foi, pode ignorar este e-mail: sua conta continua segura.
                """);
    }

    private void send(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        sender.send(message);
    }
}
