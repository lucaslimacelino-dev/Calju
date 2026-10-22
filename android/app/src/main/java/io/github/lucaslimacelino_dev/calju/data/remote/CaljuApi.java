package io.github.lucaslimacelino_dev.calju.data.remote;

import io.github.lucaslimacelino_dev.calju.data.remote.dto.AccountResponse;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.AuthBodies;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.SyncDtos;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.TokenResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

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

    // Sincronização: envio por recurso (PUT cria ou atualiza; DELETE exclui) e recebimento pelo cursor.

    @GET("api/v1/sync/pull")
    Call<SyncDtos.Pull> pull(@Query("cursor") long cursor, @Query("limit") int limit);

    @PUT("api/v1/goals/{id}")
    Call<SyncDtos.Goal> putGoal(@Path("id") String id, @Body SyncDtos.GoalBody body);

    @PUT("api/v1/foods/{id}")
    Call<SyncDtos.Food> putFood(@Path("id") String id, @Body SyncDtos.FoodBody body);

    @DELETE("api/v1/foods/{id}")
    Call<SyncDtos.Food> deleteFood(@Path("id") String id, @Query("version") int version);

    @PUT("api/v1/meals/{id}")
    Call<SyncDtos.Meal> putMeal(@Path("id") String id, @Body SyncDtos.MealBody body);

    @DELETE("api/v1/meals/{id}")
    Call<SyncDtos.Meal> deleteMeal(@Path("id") String id, @Query("version") int version);

    @PUT("api/v1/log-entries/{id}")
    Call<SyncDtos.LogEntry> putLogEntry(@Path("id") String id, @Body SyncDtos.LogEntryBody body);

    @DELETE("api/v1/log-entries/{id}")
    Call<SyncDtos.LogEntry> deleteLogEntry(@Path("id") String id, @Query("version") int version);
}
