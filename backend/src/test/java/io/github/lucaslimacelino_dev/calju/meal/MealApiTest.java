package io.github.lucaslimacelino_dev.calju.meal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.lucaslimacelino_dev.calju.ApiTest;

class MealApiTest extends ApiTest {

    private static final UUID ARROZ = UUID.randomUUID();
    private static final UUID FEIJAO = UUID.randomUUID();

    private static String marmita(UUID item1, UUID item2, int arrozGrams, int version) {
        return """
                {"name":"Marmita","items":[
                  {"id":"%s","foodId":"%s","quantityG":%d},
                  {"id":"%s","foodId":"%s","quantityG":100}],
                 "version":%d}
                """.formatted(item1, ARROZ, arrozGrams, item2, FEIJAO, version);
    }

    private MvcTestResult put(UUID user, UUID meal, String json) {
        return mvc.put().uri("/api/v1/meals/{id}", meal)
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult get(UUID user, String uri, Object... vars) {
        return mvc.get().uri(uri, vars).header(HttpHeaders.AUTHORIZATION, bearer(user)).exchange();
    }

    @Test
    void criarGuardaOsItensNaOrdem() {
        UUID user = newUser();
        UUID meal = UUID.randomUUID();

        MvcTestResult result = put(user, meal, marmita(UUID.randomUUID(), UUID.randomUUID(), 150, 0));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.items.length()").isEqualTo(2);
        assertThat(get(user, "/api/v1/meals/{id}", meal)).bodyJson()
                .extractingPath("$.items[0].foodId").isEqualTo(ARROZ.toString());
    }

    @Test
    void editarSubstituiOsItensEInverterAOrdemFunciona() {
        UUID user = newUser();
        UUID meal = UUID.randomUUID();
        UUID item1 = UUID.randomUUID();
        UUID item2 = UUID.randomUUID();
        put(user, meal, marmita(item1, item2, 150, 0));

        MvcTestResult result = put(user, meal, """
                {"name":"Marmita grande","items":[
                  {"id":"%s","foodId":"%s","quantityG":100},
                  {"id":"%s","foodId":"%s","quantityG":200}],
                 "version":1}
                """.formatted(item2, FEIJAO, item1, ARROZ));

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Marmita grande");
        assertThat(get(user, "/api/v1/meals/{id}", meal)).bodyJson()
                .extractingPath("$.items[0].foodId").isEqualTo(FEIJAO.toString());
    }

    @Test
    void refeicaoSemItensOuComItemRepetidoDa400() {
        UUID user = newUser();
        UUID item = UUID.randomUUID();

        assertThat(put(user, UUID.randomUUID(), """
                {"name":"Vazia","items":[],"version":0}
                """)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(user, UUID.randomUUID(), marmita(item, item, 150, 0))).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void excluirERestaurar() {
        UUID user = newUser();
        UUID meal = UUID.randomUUID();
        put(user, meal, marmita(UUID.randomUUID(), UUID.randomUUID(), 150, 0));

        assertThat(mvc.delete().uri("/api/v1/meals/{id}?version=1", meal)
                .header(HttpHeaders.AUTHORIZATION, bearer(user))).hasStatus(HttpStatus.OK);
        assertThat(get(user, "/api/v1/meals")).bodyJson().extractingPath("$.length()").isEqualTo(0);

        assertThat(put(user, meal, marmita(UUID.randomUUID(), UUID.randomUUID(), 150, 2)))
                .hasStatus(HttpStatus.OK);
        assertThat(get(user, "/api/v1/meals")).bodyJson().extractingPath("$.length()").isEqualTo(1);
    }

    @Test
    void refeicaoDeOutroUsuarioRespondeComo404() {
        UUID owner = newUser();
        UUID intruder = newUser();
        UUID meal = UUID.randomUUID();
        put(owner, meal, marmita(UUID.randomUUID(), UUID.randomUUID(), 150, 0));

        assertThat(get(intruder, "/api/v1/meals/{id}", meal)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(put(intruder, meal, marmita(UUID.randomUUID(), UUID.randomUUID(), 1, 1)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/v1/meals/{id}?version=1", meal)
                .header(HttpHeaders.AUTHORIZATION, bearer(intruder))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(owner, "/api/v1/meals/{id}", meal)).bodyJson().extractingPath("$.version").isEqualTo(1);
    }
}
