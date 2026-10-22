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

class AuthApiTest extends ApiTest {

    private static final String PASSWORD = "castanha de caju 123";

    @Autowired
    private RecordingMailer mailer;

    private static String newEmail() {
        return "pessoa-" + UUID.randomUUID() + "@teste.calju";
    }

    private MvcTestResult post(String path, String json) {
        return mvc.post().uri("/api/v1/auth" + path).contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult register(String email) {
        return post("/register", "{\"email\":\"%s\"}".formatted(email));
    }

    private MvcTestResult verify(String email, String code, String password) {
        return post("/verify", "{\"email\":\"%s\",\"code\":\"%s\",\"password\":\"%s\",\"deviceLabel\":\"Pixel\"}"
                .formatted(email, code, password));
    }

    private MvcTestResult login(String email, String password) {
        return post("/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
    }

    private MvcTestResult refresh(String refreshToken) {
        return post("/refresh", "{\"refreshToken\":\"%s\"}".formatted(refreshToken));
    }

    private static String field(MvcTestResult result, String path) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), path);
    }

    private String code(String email) {
        return mailer.lastCode(email, "VERIFY").orElseThrow();
    }

    /** Cadastro completo: pede o código e confirma com ele. Devolve a resposta com os tokens. */
    private MvcTestResult signUp(String email) {
        register(email);
        return verify(email, code(email), PASSWORD);
    }

    @Test
    void cadastroCodigoEConfirmacaoDaoTokensQueAbremAApi() throws Exception {
        String email = newEmail();

        assertThat(register(email)).hasStatus(HttpStatus.ACCEPTED);
        MvcTestResult tokens = verify(email, code(email), PASSWORD);

        assertThat(tokens).hasStatus(HttpStatus.OK);
        assertThat(tokens).bodyJson().extractingPath("$.expiresIn").isEqualTo(900);
        String access = field(tokens, "$.accessToken");
        assertThat(mvc.get().uri("/api/v1/goals").header(HttpHeaders.AUTHORIZATION, "Bearer " + access))
                .hasStatus(HttpStatus.OK);
    }

    @Test
    void emailComMaiusculasEEspacosEhOMesmoEmail() {
        String email = newEmail();
        register("  " + email.toUpperCase() + " ");

        assertThat(verify(email, code(email), PASSWORD)).hasStatus(HttpStatus.OK);
    }

    @Test
    void cadastroDeEmailJaConfirmadoRespondeIgualEAvisaODono() throws Exception {
        String email = newEmail();
        signUp(email);

        MvcTestResult again = register(email);

        assertThat(again).hasStatus(HttpStatus.ACCEPTED);
        assertThat(field(again, "$.message")).isEqualTo(field(register(newEmail()), "$.message"));
        assertThat(mailer.to(email)).extracting(RecordingMailer.Sent::kind).contains("ALREADY_REGISTERED");
        assertThat(login(email, PASSWORD)).hasStatus(HttpStatus.OK);
    }

    @Test
    void novoCodigoInvalidaOAnterior() {
        String email = newEmail();
        register(email);
        String first = code(email);
        register(email);
        String second = code(email);

        if (!first.equals(second)) {
            assertThat(verify(email, first, PASSWORD)).hasStatus(HttpStatus.BAD_REQUEST);
        }
        assertThat(verify(email, second, PASSWORD)).hasStatus(HttpStatus.OK);
    }

    @Test
    void cincoErrosInvalidamOCodigoMesmoOCerto() {
        String email = newEmail();
        register(email);
        String right = code(email);
        String wrong = right.equals("000000") ? "111111" : "000000";

        for (int i = 0; i < EmailCodeService.MAX_ATTEMPTS; i++) {
            assertThat(verify(email, wrong, PASSWORD)).hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.detail").isEqualTo(AuthService.INVALID_CODE);
        }

        assertThat(verify(email, right, PASSWORD)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void senhaCurtaDa400ENaoGastaOCodigo() {
        String email = newEmail();
        register(email);
        String code = code(email);

        assertThat(verify(email, code, "curta")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(verify(email, code, PASSWORD)).hasStatus(HttpStatus.OK);
    }

    @Test
    void loginErradoTemSempreAMesmaResposta() throws Exception {
        String verified = newEmail();
        signUp(verified);
        String unverified = newEmail();
        register(unverified);

        MvcTestResult wrongPassword = login(verified, "senha errada qualquer");
        MvcTestResult unknownEmail = login(newEmail(), PASSWORD);
        MvcTestResult notConfirmed = login(unverified, PASSWORD);

        for (MvcTestResult result : new MvcTestResult[]{wrongPassword, unknownEmail, notConfirmed}) {
            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(field(result, "$.detail")).isEqualTo(AuthService.INVALID_LOGIN);
        }
        assertThat(login(verified, PASSWORD)).hasStatus(HttpStatus.OK);
    }

    @Test
    void senhaFicaGuardadaComArgon2id() {
        String email = newEmail();
        signUp(email);

        assertThat(users.findByEmail(email).orElseThrow().getPasswordHash()).startsWith("{argon2}$argon2id$");
    }

    @Test
    void renovarTrocaORefreshEReusarOAntigoEncerraASessao() throws Exception {
        MvcTestResult tokens = signUp(newEmail());
        String first = field(tokens, "$.refreshToken");

        MvcTestResult renewed = refresh(first);
        assertThat(renewed).hasStatus(HttpStatus.OK);
        String second = field(renewed, "$.refreshToken");
        assertThat(second).isNotEqualTo(first);

        assertThat(refresh(first)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(second)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void sairEncerraASessaoMasNaoAsOutras() throws Exception {
        String email = newEmail();
        String phone = field(signUp(email), "$.refreshToken");
        String tablet = field(login(email, PASSWORD), "$.refreshToken");

        assertThat(post("/logout", "{\"refreshToken\":\"%s\"}".formatted(phone))).hasStatus(HttpStatus.NO_CONTENT);

        assertThat(refresh(phone)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(refresh(tablet)).hasStatus(HttpStatus.OK);
        assertThat(post("/logout", "{\"refreshToken\":\"desconhecido\"}")).hasStatus(HttpStatus.NO_CONTENT);
    }

    @Test
    void refreshInventadoDa401() {
        assertThat(refresh("nao-existe")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
