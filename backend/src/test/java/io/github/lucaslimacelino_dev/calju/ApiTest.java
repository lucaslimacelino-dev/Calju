package io.github.lucaslimacelino_dev.calju;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

import io.github.lucaslimacelino_dev.calju.common.RateLimiter;
import io.github.lucaslimacelino_dev.calju.user.UserEntity;
import io.github.lucaslimacelino_dev.calju.user.UserRepository;

/**
 * Base dos testes de integração da API: aplicação inteira, PostgreSQL real (Testcontainers) e
 * tokens assinados com a chave de teste. Todas as classes filhas compartilham o mesmo contexto.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, TestSecurityConfiguration.class, RecordingMailer.Config.class})
public abstract class ApiTest {

    @Autowired
    protected MockMvcTester mvc;

    @Autowired
    protected UserRepository users;

    @Autowired
    private RateLimiter rateLimiter;

    /** Cada teste começa sem tentativas contadas (o limitador guarda contadores em memória). */
    @BeforeEach
    void clearRateLimits() {
        rateLimiter.clear();
    }

    /** Cria um usuário novo (e-mail único) e devolve o id. */
    protected UUID newUser() {
        UUID id = UUID.randomUUID();
        users.save(new UserEntity(id, id + "@teste.calju", Instant.now()));
        return id;
    }

    protected static String bearer(UUID userId) {
        return "Bearer " + TestSecurityConfiguration.token(userId, "calju", Instant.now().plusSeconds(900));
    }
}
