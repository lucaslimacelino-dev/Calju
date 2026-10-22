package io.github.lucaslimacelino_dev.calju.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import io.github.lucaslimacelino_dev.calju.ApiTest;
import io.github.lucaslimacelino_dev.calju.RecordingMailer;
import io.github.lucaslimacelino_dev.calju.TestGoogleConfiguration;
import io.github.lucaslimacelino_dev.calju.user.UserEntity;

class GoogleLoginApiTest extends ApiTest {

    private static final String PASSWORD = "castanha de caju 123";

    @Autowired
    private RecordingMailer mailer;

    private MvcTestResult post(String path, String json) {
        return mvc.post().uri("/api/v1/auth" + path).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult google(String idToken) {
        return post("/google", "{\"idToken\":\"%s\",\"deviceLabel\":\"Pixel\"}".formatted(idToken));
    }

    private static String token(String subject, String email) {
        return TestGoogleConfiguration.idToken(subject, email, Map.of());
    }

    private static String sub() {
        return "google-" + UUID.randomUUID();
    }

    private void signUpWithPassword(String email) {
        post("/register", "{\"email\":\"%s\"}".formatted(email));
        String code = mailer.lastCode(email, "VERIFY").orElseThrow();
        post("/verify", "{\"email\":\"%s\",\"code\":\"%s\",\"password\":\"%s\"}".formatted(email, code, PASSWORD));
    }

    @Test
    void primeiroLoginCriaContaConfirmadaSemSenhaEOSegundoEntraNaMesma() {
        String subject = sub();
        String email = "pessoa-" + UUID.randomUUID() + "@gmail.com";

        assertThat(google(token(subject, email))).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.accessToken").isNotNull();
        UserEntity created = users.findByEmail(email).orElseThrow();
        assertThat(created.isEmailVerified()).isTrue();
        assertThat(created.getPasswordHash()).isNull();

        assertThat(google(token(subject, email))).hasStatus(HttpStatus.OK);
        assertThat(users.findByGoogleSubject(subject)).get().extracting(UserEntity::getId).isEqualTo(created.getId());
    }

    @Test
    void gmailIgualAContaComSenhaVinculaEAsDuasFormasFuncionam() {
        String email = "pessoa-" + UUID.randomUUID() + "@gmail.com";
        signUpWithPassword(email);
        UUID id = users.findByEmail(email).orElseThrow().getId();

        assertThat(google(token(sub(), email))).hasStatus(HttpStatus.OK);

        assertThat(users.findByEmail(email).orElseThrow().getId()).isEqualTo(id);
        assertThat(post("/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void emailForaDoGoogleIgualAContaExistenteNaoVincula() {
        String email = "pessoa-" + UUID.randomUUID() + "@teste.calju";
        signUpWithPassword(email);

        assertThat(google(token(sub(), email))).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.detail").isEqualTo(AuthService.GOOGLE_LINK_CONFLICT);
    }

    @Test
    void contaDoWorkspaceComDominioVincula() {
        String email = "pessoa-" + UUID.randomUUID() + "@empresa.example";
        signUpWithPassword(email);

        assertThat(google(TestGoogleConfiguration.idToken(sub(), email, Map.of("hd", "empresa.example"))))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void contaSoComGoogleExcluiConfirmandoComOGoogleDaMesmaConta() throws Exception {
        String subject = sub();
        String email = "pessoa-" + UUID.randomUUID() + "@gmail.com";
        String access = JsonPath.read(
                google(token(subject, email)).getResponse().getContentAsString(), "$.accessToken");

        assertThat(mvc.delete().uri("/api/v1/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"googleIdToken\":\"%s\"}".formatted(token(sub(), "outra@gmail.com"))))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.delete().uri("/api/v1/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.delete().uri("/api/v1/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + access)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"googleIdToken\":\"%s\"}".formatted(token(subject, email))))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(users.findByGoogleSubject(subject)).isEmpty();
    }

    @Test
    void tokenRuimEhRecusado() {
        String email = "pessoa-" + UUID.randomUUID() + "@gmail.com";

        assertThat(google(TestGoogleConfiguration.idToken(sub(), email, Map.of("email_verified", false))))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(google(TestGoogleConfiguration.idToken(sub(), email, Map.of("aud", "outro-app"))))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(google(TestGoogleConfiguration.idToken(sub(), email, Map.of("iss", "https://falso.example"))))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(google(TestGoogleConfiguration.idToken(sub(), email,
                Map.of("exp", Instant.now().minusSeconds(60), "iat", Instant.now().minusSeconds(3600)))))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(google("nao.e.um.token")).hasStatus(HttpStatus.UNAUTHORIZED)
                .bodyJson().extractingPath("$.detail").isEqualTo(AuthService.INVALID_GOOGLE);
        assertThat(users.findByEmail(email)).isEmpty();
    }
}
