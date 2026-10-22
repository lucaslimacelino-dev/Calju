package io.github.lucaslimacelino_dev.calju.data.remote;

import io.github.lucaslimacelino_dev.calju.data.remote.dto.AccountResponse;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.AuthBodies;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.TokenResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/** Rotas do backend usadas pelo app. O Retrofit gera a implementação a partir desta interface. */
public interface CaljuApi {

    @POST("api/v1/auth/register")
    Call<Void> register(@Body AuthBodies.Email body);

    @POST("api/v1/auth/verify")
    Call<TokenResponse> verify(@Body AuthBodies.CodeAndPassword body);

    @POST("api/v1/auth/login")
    Call<TokenResponse> login(@Body AuthBodies.Login body);

    @POST("api/v1/auth/password-reset")
    Call<Void> requestPasswordReset(@Body AuthBodies.Email body);

    @POST("api/v1/auth/password-reset/confirm")
    Call<TokenResponse> confirmPasswordReset(@Body AuthBodies.CodeAndPassword body);

    @POST("api/v1/auth/refresh")
    Call<TokenResponse> refresh(@Body AuthBodies.Refresh body);

    @POST("api/v1/auth/logout")
    Call<Void> logout(@Body AuthBodies.Refresh body);

    @GET("api/v1/account")
    Call<AccountResponse> account();
}
