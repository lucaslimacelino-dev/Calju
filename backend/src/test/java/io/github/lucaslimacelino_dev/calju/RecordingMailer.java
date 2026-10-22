package io.github.lucaslimacelino_dev.calju;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import io.github.lucaslimacelino_dev.calju.auth.CodeMailer;

/** Carteiro dos testes: não envia nada, só guarda as mensagens para o teste ler o código. */
public class RecordingMailer implements CodeMailer {

    public record Sent(String to, String kind, String code) {
    }

    private final List<Sent> sent = new ArrayList<>();

    @Override
    public synchronized void sendVerificationCode(String email, String code) {
        sent.add(new Sent(email, "VERIFY", code));
    }

    @Override
    public synchronized void sendResetCode(String email, String code) {
        sent.add(new Sent(email, "RESET", code));
    }

    @Override
    public synchronized void sendAlreadyRegistered(String email) {
        sent.add(new Sent(email, "ALREADY_REGISTERED", null));
    }

    public synchronized List<Sent> to(String email) {
        return sent.stream().filter(message -> message.to().equals(email)).toList();
    }

    public synchronized Optional<String> lastCode(String email, String kind) {
        List<Sent> messages = to(email).stream().filter(message -> message.kind().equals(kind)).toList();
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(messages.size() - 1).code());
    }

    @TestConfiguration(proxyBeanMethods = false)
    public static class Config {

        @Bean
        @Primary
        RecordingMailer recordingMailer() {
            return new RecordingMailer();
        }
    }
}
