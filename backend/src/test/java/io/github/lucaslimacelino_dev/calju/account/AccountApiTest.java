package io.github.lucaslimacelino_dev.calju.account;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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

class AccountApiTest extends ApiTest {

    private static final String PASSWORD = "castanha de caju 123";
    private static final String NEW_PASSWORD = "caju maduro no pé 456";

    @Autowired
    private RecordingMailer mailer;

    private record Session(String access, String refresh) {
    }

    private MvcTestResult post(String path, String json) {
        return mvc.post().uri(path).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static Session session(MvcTestResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return new Session(JsonPath.read(body, "$.accessToken"), JsonPath.read(body, "$.refreshToken"));
    }

    private Session signUp(String email, String device) throws Exception {
        post("/api/v1/auth/register", "{\"email\":\"%s\"}".formatted(email));
        String code = mailer.lastCode(email, "VERIFY").orElseThrow();
        return session(post("/api/v1/auth/verify", "{\"email\":\"%s\",\"code\":\"%s\",\"password\":\"%s\",\"deviceLabel\":\"%s\"}"
                .formatted(email, code, PASSWORD, device)));
    }

    private Session login(String email, String password, String device) throws Exception {
        MvcTestResult result = post("/api/v1/auth/login", "{\"email\":\"%s\",\"password\":\"%s\",\"deviceLabel\":\"%s\"}"
                .formatted(email, password, device));
        assertThat(result).hasStatus(HttpStatus.OK);
        return session(result);
    }

    private MvcTestResult refresh(Session session) {
        return post("/api/v1/auth/refresh", "{\"refreshToken\":\"%s\"}".formatted(session.refresh()));
    }

    private static String newEmail() {
        return "pessoa-" + UUID.randomUUID() + "@teste.calju";
    }

    @Test
    void contaMostraEmailESeTemSenha() throws Exception {
        String email = newEmail();
        Session phone = signUp(email, "Celular");

        MvcTestResult result = mvc.get().uri("/api/v1/account")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + phone.access()).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.email").isEqualTo(email);
        assertThat(result).bodyJson().extractingPath("$.hasPassword").isEqualTo(true);
    }

    @Test
    void listaOsAparelhosMarcaOAtualEEncerraOutro() throws Exception {
        String email = newEmail();
        Session phone = signUp(email, "Celular");
        Session tablet = login(email, PASSWORD, "Tablet");

        MvcTestResult list = mvc.get().uri("/api/v1/account/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + phone.access()).exchange();
        assertThat(list).bodyJson().extractingPath("$.length()").isEqualTo(2);
        assertThat(list).bodyJson().extractingPath("$[?(@.current == true)].deviceLabel")
                .asArray().containsExactly("Celular");

        String tabletId = JsonPath.<List<String>>read(list.getResponse().getContentAsString(),
                "$[?(@.deviceLabel == 'Tablet')].id").get(0);
        assertThat(mvc.delete().uri("/api/v1/account/sessions/{id}", tabletId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + phone.access())).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(refresh(tablet)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(phone)).hasStatus(HttpStatus.OK);
    }

    @Test
    void sessaoDeOutraPessoaNaoPodeSerEncerrada() throws Exception {
        Session owner = signUp(newEmail(), "Celular");
        Session intruder = signUp(newEmail(), "Outro");
        String ownerSessionId = JsonPath.<List<String>>read(mvc.get().uri("/api/v1/account/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + owner.access()).exchange()
                .getResponse().getContentAsString(), "$[*].id").get(0);

        assertThat(mvc.delete().uri("/api/v1/account/sessions/{id}", ownerSessionId)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + intruder.access())).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(refresh(owner)).hasStatus(HttpStatus.OK);
    }

    @Test
    void trocarSenhaPedeAAtualEDerrubaOsOutrosAparelhos() throws Exception {
        String email = newEmail();
        Session phone = signUp(email, "Celular");
        Session tablet = login(email, PASSWORD, "Tablet");

        assertThat(mvc.post().uri("/api/v1/account/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + phone.access())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"errada errada\",\"newPassword\":\"%s\"}".formatted(NEW_PASSWORD)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").isEqualTo(AccountService.WRONG_PASSWORD);

        assertThat(mvc.post().uri("/api/v1/account/password")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + phone.access())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(PASSWORD, NEW_PASSWORD)))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(refresh(tablet)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(phone)).hasStatus(HttpStatus.OK);
        assertThat(post("/api/v1/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        login(email, NEW_PASSWORD, "Notebook");
    }

    @Test
    void excluirAContaApagaOsDadosEEncerraTudo() throws Exception {
        String email = newEmail();
        Session phone = signUp(email, "Celular");
        UUID goal = UUID.randomUUID();
        mvc.put().uri("/api/v1/goals/{id}", goal).header(HttpHeaders.AUTHORIZATION, "Bearer " + phone.access())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"effectiveFrom\":\"2026-10-22\",\"kcal\":2000,\"version\":0}").exchange();

        assertThat(mvc.delete().uri("/api/v1/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + phone.access())
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"errada errada\"}"))
                .hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(mvc.delete().uri("/api/v1/account").header(HttpHeaders.AUTHORIZATION, "Bearer " + phone.access())
                .contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"%s\"}".formatted(PASSWORD)))
                .hasStatus(HttpStatus.NO_CONTENT);

        assertThat(users.findByEmail(email)).isEmpty();
        assertThat(mvc.get().uri("/api/v1/goals").header(HttpHeaders.AUTHORIZATION, "Bearer " + phone.access()))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(phone)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(post("/api/v1/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, PASSWORD)))
                .hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
