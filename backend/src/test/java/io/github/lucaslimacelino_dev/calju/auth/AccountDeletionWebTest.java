package io.github.lucaslimacelino_dev.calju.auth;

import static org.assertj.core.api.Assertions.assertThat;

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

/** Exclusão da conta pela página web, com código por e-mail (sem app e sem senha). */
class AccountDeletionWebTest extends ApiTest {

    private static final String PASSWORD = "castanha de caju 123";

    @Autowired
    private RecordingMailer mailer;

    private MvcTestResult post(String path, String json) {
        return mvc.post().uri("/api/v1/auth" + path).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String newEmail() {
        return "pessoa-" + UUID.randomUUID() + "@teste.calju";
    }

    /** Conta confirmada; devolve o token de acesso. */
    private String signUp(String email) throws Exception {
        post("/register", "{\"email\":\"%s\"}".formatted(email));
        String code = mailer.lastCode(email, "VERIFY").orElseThrow();
        MvcTestResult verified = post("/verify", "{\"email\":\"%s\",\"code\":\"%s\",\"password\":\"%s\"}"
                .formatted(email, code, PASSWORD));
        return JsonPath.read(verified.getResponse().getContentAsString(), "$.accessToken");
    }

    private MvcTestResult confirm(String email, String code) {
        return post("/account-deletion/confirm", "{\"email\":\"%s\",\"code\":\"%s\"}".formatted(email, code));
    }

    @Test
    void codigoCertoApagaAContaESeusDados() throws Exception {
        String email = newEmail();
        String token = signUp(email);
        MvcTestResult goal = mvc.put().uri("/api/v1/goals/{id}", UUID.randomUUID())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"effectiveFrom\":\"2026-10-23\",\"kcal\":2000,\"version\":0}")
                .exchange();
        assertThat(goal).hasStatus(HttpStatus.CREATED);

        assertThat(post("/account-deletion", "{\"email\":\"%s\"}".formatted(email))).hasStatus(HttpStatus.ACCEPTED);
        String code = mailer.lastCode(email, "DELETE").orElseThrow();

        assertThat(confirm(email, code)).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(post("/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/v1/goals").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void emailSemContaRespondeIgualENaoEnviaNada() {
        String email = newEmail();

        assertThat(post("/account-deletion", "{\"email\":\"%s\"}".formatted(email))).hasStatus(HttpStatus.ACCEPTED);
        assertThat(mailer.to(email)).isEmpty();
        assertThat(confirm(email, "123456")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void codigoErradoNaoApagaECodigoDeOutroTipoNaoServe() throws Exception {
        String email = newEmail();
        signUp(email);
        post("/password-reset", "{\"email\":\"%s\"}".formatted(email));
        String resetCode = mailer.lastCode(email, "RESET").orElseThrow();
        post("/account-deletion", "{\"email\":\"%s\"}".formatted(email));
        String deleteCode = mailer.lastCode(email, "DELETE").orElseThrow();
        String wrong = deleteCode.equals("000000") ? "000001" : "000000";

        assertThat(confirm(email, wrong)).hasStatus(HttpStatus.BAD_REQUEST);
        if (!resetCode.equals(deleteCode)) {
            assertThat(confirm(email, resetCode)).hasStatus(HttpStatus.BAD_REQUEST);
        }
        assertThat(post("/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void paginaPublicaEstaNoAr() throws Exception {
        MvcTestResult page = mvc.get().uri("/excluir-conta.html").exchange();
        assertThat(page).hasStatus(HttpStatus.OK);
        assertThat(page.getResponse().getContentAsString()).contains("Excluir a conta do CaLju");
        assertThat(mvc.get().uri("/excluir-conta.js").exchange()).hasStatus(HttpStatus.OK);
        assertThat(mvc.get().uri("/outra-pagina.html").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
