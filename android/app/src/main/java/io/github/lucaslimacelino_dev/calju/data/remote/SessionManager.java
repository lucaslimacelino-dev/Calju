package io.github.lucaslimacelino_dev.calju.data.remote;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.io.IOException;

import io.github.lucaslimacelino_dev.calju.data.remote.dto.AuthBodies;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.TokenResponse;
import retrofit2.Response;

/**
 * A sessão da conta: token de acesso só na memória (vale 15 min), refresh token no
 * {@link SessionStore}. Renovar é sincronizado: duas requisições que recebem 401 juntas não
 * gastam o mesmo refresh token duas vezes (o servidor trataria o segundo uso como roubo).
 */
public class SessionManager {

    private final SessionStore store;
    private final CaljuApi authApi;
    private final MutableLiveData<String> signedInEmail;
    private volatile String accessToken;

    public SessionManager(SessionStore store, CaljuApi authApi) {
        this.store = store;
        this.authApi = authApi;
        this.signedInEmail = new MutableLiveData<>(store.refreshToken() == null ? null : store.email());
    }

    /** E-mail da conta conectada; nulo = sem conta (o app funciona normalmente). */
    public LiveData<String> signedInEmail() {
        return signedInEmail;
    }

    public boolean isSignedIn() {
        return store.refreshToken() != null;
    }

    public String accessToken() {
        return accessToken;
    }

    public synchronized void signIn(TokenResponse tokens, String email) {
        accessToken = tokens.accessToken();
        store.save(tokens.refreshToken(), email);
        signedInEmail.postValue(email);
    }

    /**
     * Troca o refresh token por um par novo. Se outra thread já renovou enquanto esta esperava,
     * devolve o token novo sem chamar o servidor. Falha definitiva (401) encerra a sessão.
     *
     * @param staleAccessToken o token que recebeu 401
     * @return o token de acesso novo, ou nulo se a sessão acabou ou a rede falhou
     */
    public synchronized String refresh(String staleAccessToken) {
        if (accessToken != null && !accessToken.equals(staleAccessToken)) {
            return accessToken;
        }
        String refreshToken = store.refreshToken();
        if (refreshToken == null) {
            return null;
        }
        try {
            Response<TokenResponse> response = authApi.refresh(new AuthBodies.Refresh(refreshToken)).execute();
            if (response.isSuccessful() && response.body() != null) {
                accessToken = response.body().accessToken();
                store.save(response.body().refreshToken(), store.email());
                return accessToken;
            }
            if (response.code() == 401) {
                clearLocalSession();
            }
            return null;
        } catch (IOException e) {
            return null;
        }
    }

    /** Sai no servidor (melhor esforço) e apaga a sessão do aparelho. Os dados locais ficam. */
    public void signOut() {
        String refreshToken = store.refreshToken();
        if (refreshToken != null) {
            try {
                authApi.logout(new AuthBodies.Refresh(refreshToken)).execute();
            } catch (IOException ignored) {
                // Sem rede: o token expira sozinho no servidor; aqui a sessão já é apagada.
            }
        }
        clearLocalSession();
    }

    private synchronized void clearLocalSession() {
        accessToken = null;
        store.clear();
        signedInEmail.postValue(null);
    }
}
