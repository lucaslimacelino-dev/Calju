package io.github.lucaslimacelino_dev.calju.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.lucaslimacelino_dev.calju.ApiTest;
import io.github.lucaslimacelino_dev.calju.RecordingMailer;

class RateLimitApiTest extends ApiTest {

    private static final String PASSWORD = "castanha de caju 123";

    @Autowired
    private RecordingMailer mailer;

    private MvcTestResult post(String ip, String path, String json) {
        return mvc.post().uri("/api/v1/auth" + path)
                .with(request -> {
                    request.setRemoteAddr(ip);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult login(String ip, String email, String password) {
        return post(ip, "/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private static String newEmail() {
        return "pessoa-" + UUID.randomUUID() + "@teste.calju";
    }

    private void signUp(String email) {
        post("10.0.0.1", "/register", "{\"email\":\"%s\"}".formatted(email));
        String code = mailer.lastCode(email, "VERIFY").orElseThrow();
        post("10.0.0.1", "/verify", "{\"email\":\"%s\",\"code\":\"%s\",\"password\":\"%s\"}"
                .formatted(email, code, PASSWORD));
    }

    @Test
    void muitasSenhasErradasDoMesmoIpDao429ComRetryAfter() {
        String email = newEmail();
        signUp(email);

        for (int i = 0; i < 10; i++) {
            assertThat(login("10.0.0.66", email, "senha errada " + i)).hasStatus(HttpStatus.UNAUTHORIZED);
        }
        MvcTestResult blocked = login("10.0.0.66", email, PASSWORD);

        assertThat(blocked).hasStatus(HttpStatus.TOO_MANY_REQUESTS)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(blocked).headers().containsHeader(HttpHeaders.RETRY_AFTER);
    }

    @Test
    void atacanteNaoTrancaAContaDoDono() {
        String email = newEmail();
        signUp(email);
        for (int i = 0; i < 11; i++) {
            login("10.0.0.66", email, "senha errada " + i);
        }

        assertThat(login("10.0.0.7", email, PASSWORD)).hasStatus(HttpStatus.OK);
    }

    @Test
    void cadastroTemTetoPorEmailContraEnchenteDeEmails() {
        String email = newEmail();
        for (int i = 0; i < 5; i++) {
            assertThat(post("10.0.1." + i, "/register", "{\"email\":\"%s\"}".formatted(email)))
                    .hasStatus(HttpStatus.ACCEPTED);
        }

        assertThat(post("10.0.1.99", "/register", "{\"email\":\"%s\"}".formatted(email)))
                .hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(mailer.to(email)).hasSize(5);
    }
}
