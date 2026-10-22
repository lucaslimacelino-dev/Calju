package io.github.lucaslimacelino_dev.calju.data.repository;

import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;

import java.io.IOException;
import java.util.Locale;

import io.github.lucaslimacelino_dev.calju.data.remote.ApiClient;
import io.github.lucaslimacelino_dev.calju.data.remote.ApiErrors;
import io.github.lucaslimacelino_dev.calju.data.remote.SessionManager;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.AuthBodies;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.TokenResponse;
import retrofit2.Call;
import retrofit2.Response;

/**
 * Conta opcional: cadastro com código, login, redefinição de senha e saída. Cada chamada vai ao
 * servidor, então deve rodar fora da thread da tela. Os dados locais nunca dependem da conta.
 */
public class AccountRepository {

    /** Resultado para a tela: {@code error} nulo = deu certo. */
    public record Result(String error) {

        public static final Result OK = new Result(null);

        public boolean ok() {
            return error == null;
        }
    }

    private final ApiClient api;
    private final String deviceLabel;

    public AccountRepository(ApiClient api, String deviceLabel) {
        this.api = api;
        this.deviceLabel = deviceLabel;
    }

    public LiveData<String> signedInEmail() {
        return api.session().signedInEmail();
    }

    public SessionManager session() {
        return api.session();
    }

    /** Envia o código para criar a conta (o servidor responde igual exista ou não a conta). */
    @WorkerThread
    public Result requestSignUpCode(String email) {
        return call(api.auth().register(new AuthBodies.Email(email.trim())));
    }

    @WorkerThread
    public Result confirmSignUp(String email, String code, String password) {
        return signIn(api.auth().verify(new AuthBodies.CodeAndPassword(email.trim(), code, password, deviceLabel)),
                email);
    }

    @WorkerThread
    public Result login(String email, String password) {
        return signIn(api.auth().login(new AuthBodies.Login(email.trim(), password, deviceLabel)), email);
    }

    @WorkerThread
    public Result requestPasswordReset(String email) {
        return call(api.auth().requestPasswordReset(new AuthBodies.Email(email.trim())));
    }

    @WorkerThread
    public Result confirmPasswordReset(String email, String code, String password) {
        return signIn(api.auth().confirmPasswordReset(
                new AuthBodies.CodeAndPassword(email.trim(), code, password, deviceLabel)), email);
    }

    @WorkerThread
    public void signOut() {
        api.session().signOut();
    }

    private Result signIn(Call<TokenResponse> call, String email) {
        try {
            Response<TokenResponse> response = call.execute();
            if (response.isSuccessful() && response.body() != null) {
                api.session().signIn(response.body(), email.trim().toLowerCase(Locale.ROOT));
                return Result.OK;
            }
            return new Result(ApiErrors.message(response));
        } catch (IOException e) {
            return new Result(ApiErrors.NO_CONNECTION);
        }
    }

    private static Result call(Call<Void> call) {
        try {
            Response<Void> response = call.execute();
            return response.isSuccessful() ? Result.OK : new Result(ApiErrors.message(response));
        } catch (IOException e) {
            return new Result(ApiErrors.NO_CONNECTION);
        }
    }
}
