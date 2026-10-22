package io.github.lucaslimacelino_dev.calju.data.remote;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;

import java.io.IOException;

import io.github.lucaslimacelino_dev.calju.data.remote.dto.ProblemResponse;
import okhttp3.ResponseBody;
import retrofit2.Response;

/** Transforma respostas de erro do backend (ProblemDetail) em texto para mostrar ao usuário. */
public final class ApiErrors {

    public static final String NO_CONNECTION = "Sem conexão com o servidor. Confira a internet e tente de novo.";
    public static final String UNEXPECTED = "Algo deu errado no servidor. Tente de novo em instantes.";

    private static final Gson GSON = new Gson();

    private ApiErrors() {
    }

    /** O "detail" do ProblemDetail, quando houver; senão uma mensagem genérica pelo código. */
    public static String message(Response<?> response) {
        ResponseBody body = response.errorBody();
        if (body != null) {
            try {
                ProblemResponse problem = GSON.fromJson(body.string(), ProblemResponse.class);
                if (problem != null && problem.detail() != null && !problem.detail().isBlank()) {
                    return problem.detail();
                }
            } catch (IOException | JsonParseException ignored) {
                // Corpo ilegível: cai na mensagem genérica.
            }
        }
        return UNEXPECTED;
    }
}
