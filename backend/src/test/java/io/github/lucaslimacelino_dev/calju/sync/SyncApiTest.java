package io.github.lucaslimacelino_dev.calju.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import com.jayway.jsonpath.JsonPath;

import io.github.lucaslimacelino_dev.calju.ApiTest;

class SyncApiTest extends ApiTest {

    private void put(UUID user, String path, UUID id, String json) {
        assertThat(mvc.put().uri(path + "/{id}", id).header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON).content(json)).hasStatus2xxSuccessful();
    }

    private MvcTestResult pull(UUID user, long cursor, int limit) {
        return mvc.get().uri("/api/v1/sync/pull?cursor={c}&limit={l}", cursor, limit)
                .header(HttpHeaders.AUTHORIZATION, bearer(user)).exchange();
    }

    private static long cursorOf(MvcTestResult result) throws Exception {
        return ((Number) JsonPath.read(result.getResponse().getContentAsString(), "$.cursor")).longValue();
    }

    private static String goal(int version) {
        return "{\"effectiveFrom\":\"2026-10-22\",\"kcal\":2000,\"version\":%d}".formatted(version);
    }

    private static String food(int version) {
        return """
                {"name":"Granola","nutrients":[{"code":"ENERC","amount":400,"qualifier":"VALUE"}],"version":%d}
                """.formatted(version);
    }

    private static String meal() {
        return """
                {"name":"Marmita","items":[{"id":"%s","foodId":"%s","quantityG":150}],"version":0}
                """.formatted(UUID.randomUUID(), UUID.randomUUID());
    }

    private static String entry() {
        return """
                {"logDate":"2026-10-22","occasion":"LUNCH","createdAt":"2026-10-22T12:00:00Z",
                 "items":[{"id":"%s","foodName":"Arroz","quantityG":150,"nutrients":[]}],"version":0}
                """.formatted(UUID.randomUUID());
    }

    @Test
    void primeiraVezTrazTudoEDepoisSoONovo() throws Exception {
        UUID user = newUser();
        UUID foodId = UUID.randomUUID();
        put(user, "/api/v1/goals", UUID.randomUUID(), goal(0));
        put(user, "/api/v1/foods", foodId, food(0));
        put(user, "/api/v1/meals", UUID.randomUUID(), meal());
        put(user, "/api/v1/log-entries", UUID.randomUUID(), entry());

        MvcTestResult first = pull(user, 0, 500);
        assertThat(first).hasStatus(HttpStatus.OK);
        assertThat(first).bodyJson().extractingPath("$.goals.length()").isEqualTo(1);
        assertThat(first).bodyJson().extractingPath("$.foods.length()").isEqualTo(1);
        assertThat(first).bodyJson().extractingPath("$.meals.length()").isEqualTo(1);
        assertThat(first).bodyJson().extractingPath("$.logEntries.length()").isEqualTo(1);
        assertThat(first).bodyJson().extractingPath("$.hasMore").isEqualTo(false);
        long cursor = cursorOf(first);

        MvcTestResult nothingNew = pull(user, cursor, 500);
        assertThat(nothingNew).bodyJson().extractingPath("$.foods.length()").isEqualTo(0);
        assertThat(cursorOf(nothingNew)).isEqualTo(cursor);

        put(user, "/api/v1/foods", foodId, food(1));
        MvcTestResult changed = pull(user, cursor, 500);
        assertThat(changed).bodyJson().extractingPath("$.foods[0].version").isEqualTo(2);
        assertThat(changed).bodyJson().extractingPath("$.goals.length()").isEqualTo(0);
        assertThat(cursorOf(changed)).isGreaterThan(cursor);
    }

    @Test
    void exclusaoChegaComoLapide() throws Exception {
        UUID user = newUser();
        UUID foodId = UUID.randomUUID();
        put(user, "/api/v1/foods", foodId, food(0));
        long cursor = cursorOf(pull(user, 0, 500));

        mvc.delete().uri("/api/v1/foods/{id}?version=1", foodId).header(HttpHeaders.AUTHORIZATION, bearer(user))
                .exchange();

        MvcTestResult result = pull(user, cursor, 500);
        assertThat(result).bodyJson().extractingPath("$.foods[0].id").isEqualTo(foodId.toString());
        assertThat(result).bodyJson().extractingPath("$.foods[0].deletedAt").isNotNull();
    }

    @Test
    void dadosDeOutroUsuarioNuncaAparecem() {
        UUID owner = newUser();
        UUID other = newUser();
        put(owner, "/api/v1/foods", UUID.randomUUID(), food(0));

        MvcTestResult result = pull(other, 0, 500);
        assertThat(result).bodyJson().extractingPath("$.foods.length()").isEqualTo(0);
        assertThat(result).bodyJson().extractingPath("$.cursor").isEqualTo(0);
    }

    @Test
    void paginasSeguemOCursorSemPerderNada() throws Exception {
        UUID user = newUser();
        for (int i = 0; i < 5; i++) {
            put(user, "/api/v1/foods", UUID.randomUUID(), food(0));
        }

        MvcTestResult page1 = pull(user, 0, 2);
        MvcTestResult page2 = pull(user, cursorOf(page1), 2);
        MvcTestResult page3 = pull(user, cursorOf(page2), 2);

        assertThat(page1).bodyJson().extractingPath("$.hasMore").isEqualTo(true);
        assertThat(page2).bodyJson().extractingPath("$.hasMore").isEqualTo(true);
        assertThat(page3).bodyJson().extractingPath("$.hasMore").isEqualTo(false);
        assertThat(page3).bodyJson().extractingPath("$.foods.length()").isEqualTo(1);
    }

    @Test
    void parametrosInvalidosDao400() {
        UUID user = newUser();

        assertThat(pull(user, -1, 10)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(pull(user, 0, 5000)).hasStatus(HttpStatus.BAD_REQUEST);
    }
}
