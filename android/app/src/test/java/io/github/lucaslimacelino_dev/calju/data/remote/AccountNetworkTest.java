package io.github.lucaslimacelino_dev.calju.data.remote;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.github.lucaslimacelino_dev.calju.data.repository.AccountRepository;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.Response;

/** Rede da conta contra um servidor simulado (MockWebServer): sucesso, erros, renovação e saída. */
public class AccountNetworkTest {

    @Rule
    public InstantTaskExecutorRule liveDataRule = new InstantTaskExecutorRule();

    /** Guarda a sessão na memória (o Keystore só existe no Android). */
    static final class MemoryStore implements SessionStore {
        String token;
        String email;

        @Override
        public String refreshToken() {
            return token;
        }

        @Override
        public String email() {
            return email;
        }

        @Override
        public void save(String refreshToken, String email) {
            this.token = refreshToken;
            this.email = email;
        }

        @Override
        public void clear() {
            token = null;
            email = null;
        }
    }

    private MockWebServer server;
    private MemoryStore store;
    private ApiClient api;
    private AccountRepository account;

    private static MockResponse tokens(String access, String refresh) {
        return new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody("{\"accessToken\":\"" + access + "\",\"tokenType\":\"Bearer\",\"expiresIn\":900,"
                        + "\"refreshToken\":\"" + refresh + "\",\"refreshExpiresIn\":2592000}");
    }

    private static MockResponse problem(int status, String detail) {
        return new MockResponse().setResponseCode(status).setHeader("Content-Type", "application/problem+json")
                .setBody("{\"title\":\"x\",\"status\":" + status + ",\"detail\":\"" + detail + "\"}");
    }

    @Before
    public void setUp() throws Exception {
        server = new MockWebServer();
        server.start();
        store = new MemoryStore();
        api = new ApiClient(server.url("/").toString(), store);
        account = new AccountRepository(api, "Teste");
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
    }

    @Test
    public void loginGuardaASessaoComOEmailNormalizado() throws Exception {
        server.enqueue(tokens("acesso-1", "refresh-1"));

        AccountRepository.Result result = account.login(" Pessoa@Exemplo.com ", "castanha de caju 123");

        assertTrue(result.ok());
        assertEquals("refresh-1", store.token);
        assertEquals("pessoa@exemplo.com", store.email);
        assertEquals("pessoa@exemplo.com", account.signedInEmail().getValue());
        RecordedRequest request = server.takeRequest();
        assertEquals("/api/v1/auth/login", request.getPath());
        assertTrue(request.getBody().readUtf8().contains("\"deviceLabel\":\"Teste\""));
    }

    @Test
    public void erroDoServidorMostraODetalheDoProblemDetail() {
        server.enqueue(problem(401, "E-mail ou senha incorretos."));

        AccountRepository.Result result = account.login("pessoa@exemplo.com", "errada errada");

        assertEquals("E-mail ou senha incorretos.", result.error());
        assertNull(store.token);
    }

    @Test
    public void servidorForaDoArViraMensagemDeConexao() throws Exception {
        server.shutdown();

        assertEquals(ApiErrors.NO_CONNECTION, account.requestSignUpCode("pessoa@exemplo.com").error());
    }

    @Test
    public void tokenVencidoRenovaSozinhoERepeteARequisicao() throws Exception {
        server.enqueue(tokens("acesso-velho", "refresh-1"));
        account.login("pessoa@exemplo.com", "castanha de caju 123");
        server.takeRequest();

        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(tokens("acesso-novo", "refresh-2"));
        server.enqueue(new MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setBody("{\"id\":\"1\",\"email\":\"pessoa@exemplo.com\",\"hasPassword\":true}"));

        Response<?> response = api.data().account().execute();

        assertTrue(response.isSuccessful());
        assertEquals("Bearer acesso-velho", server.takeRequest().getHeader("Authorization"));
        assertEquals("/api/v1/auth/refresh", server.takeRequest().getPath());
        assertEquals("Bearer acesso-novo", server.takeRequest().getHeader("Authorization"));
        assertEquals("refresh-2", store.token);
    }

    @Test
    public void renovacaoRecusadaEncerraASessaoNoAparelho() throws Exception {
        server.enqueue(tokens("acesso-velho", "refresh-1"));
        account.login("pessoa@exemplo.com", "castanha de caju 123");

        server.enqueue(new MockResponse().setResponseCode(401));
        server.enqueue(problem(401, "Sessão inválida ou expirada."));

        Response<?> response = api.data().account().execute();

        assertEquals(401, response.code());
        assertFalse(api.session().isSignedIn());
        assertNull(account.signedInEmail().getValue());
    }

    @Test
    public void sairAvisaOServidorEApagaASessao() throws Exception {
        server.enqueue(tokens("acesso", "refresh-1"));
        account.login("pessoa@exemplo.com", "castanha de caju 123");
        server.takeRequest();
        server.enqueue(new MockResponse().setResponseCode(204));

        account.signOut();

        RecordedRequest logout = server.takeRequest();
        assertEquals("/api/v1/auth/logout", logout.getPath());
        assertTrue(logout.getBody().readUtf8().contains("refresh-1"));
        assertNull(store.token);
    }
}
