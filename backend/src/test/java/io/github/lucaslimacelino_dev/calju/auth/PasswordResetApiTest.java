package io.github.lucaslimacelino_dev.calju.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import io.github.lucaslimacelino_dev.calju.ApiTest;
import io.github.lucaslimacelino_dev.calju.RecordingMailer;

class PasswordResetApiTest extends ApiTest {

    private static final String OLD_PASSWORD = "castanha de caju 123";
    private static final String NEW_PASSWORD = "caju maduro no pé 456";

    @Autowired
    private RecordingMailer mailer;

    private MvcTestResult post(String path, String json) {
        return mvc.post().uri("/api/v1/auth" + path).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String field(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    /** Conta confirmada com OLD_PASSWORD; devolve o refresh token da primeira sessão. */
    private String signUp(String email) throws Exception {
        post("/register", "{\"email\":\"%s\"}".formatted(email));
        String code = mailer.lastCode(email, "VERIFY").orElseThrow();
        return field(post("/verify", "{\"email\":\"%s\",\"code\":\"%s\",\"password\":\"%s\"}"
                .formatted(email, code, OLD_PASSWORD)), "$.refreshToken");
    }

    private MvcTestResult requestReset(String email) {
        return post("/password-reset", "{\"email\":\"%s\"}".formatted(email));
    }

    private MvcTestResult confirm(String email, String code, String password) {
        return post("/password-reset/confirm", "{\"email\":\"%s\",\"code\":\"%s\",\"password\":\"%s\"}"
                .formatted(email, code, password));
    }

    private MvcTestResult login(String email, String password) {
        return post("/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private static String newEmail() {
        return "pessoa-" + UUID.randomUUID() + "@teste.calju";
    }

    @Test
    void redefinirTrocaASenhaEEncerraAsSessoesAntigas() throws Exception {
        String email = newEmail();
        String oldSession = signUp(email);

        assertThat(requestReset(email)).hasStatus(HttpStatus.ACCEPTED);
        MvcTestResult result = confirm(email, mailer.lastCode(email, "RESET").orElseThrow(), NEW_PASSWORD);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(login(email, OLD_PASSWORD)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(login(email, NEW_PASSWORD)).hasStatus(HttpStatus.OK);
        assertThat(post("/refresh", "{\"refreshToken\":\"%s\"}".formatted(oldSession)))
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(post("/refresh", "{\"refreshToken\":\"%s\"}".formatted(field(result, "$.refreshToken"))))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void emailSemContaConfirmadaRespondeIgualENaoRecebeCodigo() throws Exception {
        String unknown = newEmail();
        String unverified = newEmail();
        post("/register", "{\"email\":\"%s\"}".formatted(unverified));
        String confirmed = newEmail();
        signUp(confirmed);

        String expected = field(requestReset(confirmed), "$.message");
        assertThat(field(requestReset(unknown), "$.message")).isEqualTo(expected);
        assertThat(field(requestReset(unverified), "$.message")).isEqualTo(expected);

        assertThat(mailer.lastCode(unknown, "RESET")).isEmpty();
        assertThat(mailer.lastCode(unverified, "RESET")).isEmpty();
        assertThat(mailer.lastCode(confirmed, "RESET")).isPresent();
    }

    @Test
    void codigoDeCadastroNaoServeParaRedefinirENemOContrario() throws Exception {
        String email = newEmail();
        post("/register", "{\"email\":\"%s\"}".formatted(email));
        String verifyCode = mailer.lastCode(email, "VERIFY").orElseThrow();
        post("/verify", "{\"email\":\"%s\",\"code\":\"%s\",\"password\":\"%s\"}".formatted(email, verifyCode, OLD_PASSWORD));

        requestReset(email);
        String resetCode = mailer.lastCode(email, "RESET").orElseThrow();

        if (!verifyCode.equals(resetCode)) {
            assertThat(confirm(email, verifyCode, NEW_PASSWORD)).hasStatus(HttpStatus.BAD_REQUEST);
        }
        assertThat(confirm(email, resetCode, NEW_PASSWORD)).hasStatus(HttpStatus.OK);
        assertThat(confirm(email, resetCode, OLD_PASSWORD)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void codigoErradoDa400Generico() throws Exception {
        String email = newEmail();
        signUp(email);
        requestReset(email);
        String right = mailer.lastCode(email, "RESET").orElseThrow();
        String wrong = right.equals("000000") ? "111111" : "000000";

        assertThat(confirm(email, wrong, NEW_PASSWORD)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").isEqualTo(AuthService.INVALID_CODE);
        assertThat(confirm(newEmail(), right, NEW_PASSWORD)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").isEqualTo(AuthService.INVALID_CODE);
        assertThat(login(email, OLD_PASSWORD)).hasStatus(HttpStatus.OK);
    }
}
