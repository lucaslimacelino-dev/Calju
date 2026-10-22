package io.github.lucaslimacelino_dev.calju;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/** Padrão negar: sem token válido, nada além do health responde. */
class SecurityTest extends ApiTest {

    private static final String GOALS = "/api/v1/goals";

    @Test
    void semTokenResponde401EmProblemDetail() {
        assertThat(mvc.get().uri(GOALS))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().extractingPath("$.status").isEqualTo(401);
    }

    @Test
    void tokenValidoDeUsuarioExistenteEntra() {
        UUID user = newUser();

        assertThat(mvc.get().uri(GOALS).header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void tokenExpiradoEhRecusado() {
        String token = TestSecurityConfiguration.token(newUser(), "calju", Instant.now().minusSeconds(3600));

        assertThat(mvc.get().uri(GOALS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenDeOutroEmissorEhRecusado() {
        String token = TestSecurityConfiguration.token(newUser(), "outro", Instant.now().plusSeconds(900));

        assertThat(mvc.get().uri(GOALS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenAssinadoComOutraChaveEhRecusado() {
        byte[] otherKey = new byte[32];
        new SecureRandom().nextBytes(otherKey);
        String token = TestSecurityConfiguration.token(newUser().toString(), "calju", Instant.now().plusSeconds(900),
                new SecretKeySpec(otherKey, "HmacSHA256"));

        assertThat(mvc.get().uri(GOALS).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void tokenDeContaInexistenteEhRecusado() {
        assertThat(mvc.get().uri(GOALS).header(HttpHeaders.AUTHORIZATION, bearer(UUID.randomUUID())))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.detail").isEqualTo("Conta não encontrada.");
    }

    @Test
    void healthContinuaPublico() {
        assertThat(mvc.get().uri("/actuator/health")).hasStatus(HttpStatus.OK);
    }
}
