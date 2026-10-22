package io.github.lucaslimacelino_dev.calju.data.remote;

import java.util.concurrent.TimeUnit;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Monta os clientes HTTP. {@link #auth()} fala com as rotas de conta (sem renovação automática);
 * {@link #data()} põe o token de acesso em cada requisição e, ao receber 401, renova e tenta de
 * novo uma única vez.
 */
public class ApiClient {

    private static final String BEARER = "Bearer ";

    private final CaljuApi auth;
    private final CaljuApi data;
    private final SessionManager session;

    public ApiClient(String baseUrl, SessionStore store) {
        OkHttpClient plain = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.auth = retrofit(baseUrl, plain);
        this.session = new SessionManager(store, auth);
        OkHttpClient authorized = plain.newBuilder()
                .addInterceptor(bearer())
                .authenticator(refreshOn401())
                .build();
        this.data = retrofit(baseUrl, authorized);
    }

    public CaljuApi auth() {
        return auth;
    }

    public CaljuApi data() {
        return data;
    }

    public SessionManager session() {
        return session;
    }

    private static CaljuApi retrofit(String baseUrl, OkHttpClient client) {
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CaljuApi.class);
    }

    private Interceptor bearer() {
        return chain -> {
            String token = session.accessToken();
            if (token == null) {
                // Ainda sem token na memória (app acabou de abrir): renova antes de tentar.
                token = session.refresh(null);
            }
            Request request = chain.request();
            if (token != null) {
                request = request.newBuilder().header("Authorization", BEARER + token).build();
            }
            return chain.proceed(request);
        };
    }

    /** 401 numa requisição com token: renova e repete; se já repetiu ou não conseguiu, desiste. */
    private Authenticator refreshOn401() {
        return (route, response) -> {
            if (response.priorResponse() != null) {
                return null;
            }
            String sent = response.request().header("Authorization");
            String stale = sent != null && sent.startsWith(BEARER) ? sent.substring(BEARER.length()) : null;
            String fresh = session.refresh(stale);
            return fresh == null ? null
                    : response.request().newBuilder().header("Authorization", BEARER + fresh).build();
        };
    }
}
