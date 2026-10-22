package io.github.lucaslimacelino_dev.calju.data.remote;

/** Onde fica a sessão entre aberturas do app: o refresh token (secreto) e o e-mail (para exibir). */
public interface SessionStore {

    /** Nulo se não há sessão (ou se o token guardado não pôde ser lido). */
    String refreshToken();

    String email();

    void save(String refreshToken, String email);

    void clear();
}
