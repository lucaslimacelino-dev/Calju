package io.github.lucaslimacelino_dev.calju.diary;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

import io.github.lucaslimacelino_dev.calju.ApiTest;

class LogEntryApiTest extends ApiTest {

    private static final UUID ARROZ = UUID.randomUUID();

    // Valores fictícios por 100 g: só o comportamento importa.
    private static String item(UUID id, String name, int grams) {
        return """
                {"id":"%s","foodId":"%s","foodName":"%s","brand":null,"source":"TACO-4",
                 "quantityG":%d,"portionLabel":null,
                 "nutrients":[{"code":"ENERC","amount":128,"qualifier":"VALUE"},
                              {"code":"FE","amount":null,"qualifier":"TRACE"}]}
                """.formatted(id, ARROZ, name, grams);
    }

    private static String entry(String date, String occasion, int version, String... items) {
        return """
                {"logDate":"%s","occasion":"%s","title":null,"sourceMealId":null,
                 "createdAt":"2026-10-22T12:40:00Z","items":[%s],"version":%d}
                """.formatted(date, occasion, String.join(",", items), version);
    }

    private static Predicate<BigDecimal> same(String expected) {
        return value -> value.compareTo(new BigDecimal(expected)) == 0;
    }

    private MvcTestResult put(UUID user, UUID entry, String json) {
        return mvc.put().uri("/api/v1/log-entries/{id}", entry)
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult get(UUID user, String uri, Object... vars) {
        return mvc.get().uri(uri, vars).header(HttpHeaders.AUTHORIZATION, bearer(user)).exchange();
    }

    @Test
    void criarGuardaOSnapshotComTraco() {
        UUID user = newUser();
        UUID entry = UUID.randomUUID();

        MvcTestResult result = put(user, entry, entry("2026-10-22", "LUNCH", 0,
                item(UUID.randomUUID(), "Arroz, tipo 1, cozido", 150)));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.items[0].foodName").isEqualTo("Arroz, tipo 1, cozido");
        assertThat(result).bodyJson().extractingPath("$.items[0].nutrients[?(@.code=='FE')].qualifier")
                .asArray().containsExactly("TRACE");
        assertThat(get(user, "/api/v1/log-entries/{id}", entry)).bodyJson()
                .extractingPath("$.items[0].quantityG").convertTo(BigDecimal.class).matches(same("150"));
    }

    @Test
    void unidadeDoItemPadraoGramaEMlGuardada() {
        UUID user = newUser();
        UUID entry = UUID.randomUUID();
        String suco = item(UUID.randomUUID(), "Suco de caju", 200).replace("\"quantityG\"", "\"unit\":\"ml\",\"quantityG\"");

        MvcTestResult result = put(user, entry, entry("2026-10-22", "LUNCH", 0,
                item(UUID.randomUUID(), "Arroz, tipo 1, cozido", 150), suco));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.items[0].unit").isEqualTo("g");
        assertThat(get(user, "/api/v1/log-entries/{id}", entry)).bodyJson()
                .extractingPath("$.items[1].unit").isEqualTo("ml");
    }

    @Test
    void pesoDaPorcaoVaiEVoltaNoItem() {
        UUID user = newUser();
        UUID entry = UUID.randomUUID();
        String banana = item(UUID.randomUUID(), "Banana-prata", 300)
                .replace("\"portionLabel\":null", "\"portionLabel\":\"1 unidade\",\"portionGrams\":75");

        assertThat(put(user, entry, entry("2026-10-25", "SNACK", 0, banana))).hasStatus(HttpStatus.CREATED);

        MvcTestResult stored = get(user, "/api/v1/log-entries/{id}", entry);
        assertThat(stored).bodyJson().extractingPath("$.items[0].portionLabel").isEqualTo("1 unidade");
        assertThat(stored).bodyJson().extractingPath("$.items[0].portionGrams").convertTo(BigDecimal.class)
                .matches(same("75"));
    }

    @Test
    void editarQuantidadeEInverterAOrdemDosItens() {
        UUID user = newUser();
        UUID entry = UUID.randomUUID();
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        put(user, entry, entry("2026-10-22", "LUNCH", 0, item(first, "Arroz", 150), item(second, "Feijão", 100)));

        MvcTestResult result = put(user, entry, entry("2026-10-22", "DINNER", 1,
                item(second, "Feijão", 100), item(first, "Arroz", 50)));

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.occasion").isEqualTo("DINNER");
        MvcTestResult stored = get(user, "/api/v1/log-entries/{id}", entry);
        assertThat(stored).bodyJson().extractingPath("$.items[0].id").isEqualTo(second.toString());
        assertThat(stored).bodyJson().extractingPath("$.items[1].quantityG").convertTo(BigDecimal.class)
                .matches(same("50"));
    }

    @Test
    void itemQueNaoVemESaiDoRegistro() {
        UUID user = newUser();
        UUID entry = UUID.randomUUID();
        UUID kept = UUID.randomUUID();
        put(user, entry, entry("2026-10-22", "LUNCH", 0, item(kept, "Arroz", 150), item(UUID.randomUUID(), "X", 10)));

        assertThat(put(user, entry, entry("2026-10-22", "LUNCH", 1, item(kept, "Arroz", 150))))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.items.length()").isEqualTo(1);
    }

    @Test
    void periodoTrazSoOsRegistrosAtivosDoUsuario() {
        UUID user = newUser();
        UUID other = newUser();
        put(user, UUID.randomUUID(), entry("2026-10-20", "BREAKFAST", 0, item(UUID.randomUUID(), "A", 10)));
        put(user, UUID.randomUUID(), entry("2026-10-22", "LUNCH", 0, item(UUID.randomUUID(), "B", 10)));
        put(user, UUID.randomUUID(), entry("2026-11-05", "LUNCH", 0, item(UUID.randomUUID(), "C", 10)));
        UUID removed = UUID.randomUUID();
        put(user, removed, entry("2026-10-21", "SNACK", 0, item(UUID.randomUUID(), "D", 10)));
        mvc.delete().uri("/api/v1/log-entries/{id}?version=1", removed)
                .header(HttpHeaders.AUTHORIZATION, bearer(user)).exchange();
        put(other, UUID.randomUUID(), entry("2026-10-21", "LUNCH", 0, item(UUID.randomUUID(), "E", 10)));

        MvcTestResult result = get(user, "/api/v1/log-entries?from=2026-10-01&to=2026-10-31");

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$[*].items[0].foodName")
                .asArray().containsExactly("A", "B");
    }

    @Test
    void periodoInvertidoOuLongoDemaisDa400() {
        UUID user = newUser();

        assertThat(get(user, "/api/v1/log-entries?from=2026-10-22&to=2026-10-01")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(get(user, "/api/v1/log-entries?from=2025-01-01&to=2026-10-22")).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(get(user, "/api/v1/log-entries?from=2026-10-01")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void registroSemItensOuComOcasiaoInvalidaDa400() {
        UUID user = newUser();

        assertThat(put(user, UUID.randomUUID(), entry("2026-10-22", "LUNCH", 0))).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(user, UUID.randomUUID(), entry("2026-10-22", "BRUNCH", 0, item(UUID.randomUUID(), "A", 10))))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void excluirERestaurarComODesfazer() {
        UUID user = newUser();
        UUID entry = UUID.randomUUID();
        UUID item = UUID.randomUUID();
        put(user, entry, entry("2026-10-22", "LUNCH", 0, item(item, "Arroz", 150)));

        assertThat(mvc.delete().uri("/api/v1/log-entries/{id}?version=1", entry)
                .header(HttpHeaders.AUTHORIZATION, bearer(user))).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.deletedAt").isNotNull();

        assertThat(put(user, entry, entry("2026-10-22", "LUNCH", 2, item(item, "Arroz", 150))))
                .hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.deletedAt").isNull();
    }

    @Test
    void registroDeOutroUsuarioRespondeComo404() {
        UUID owner = newUser();
        UUID intruder = newUser();
        UUID entry = UUID.randomUUID();
        put(owner, entry, entry("2026-10-22", "LUNCH", 0, item(UUID.randomUUID(), "Arroz", 150)));

        assertThat(get(intruder, "/api/v1/log-entries/{id}", entry)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(put(intruder, entry, entry("2026-10-22", "LUNCH", 1, item(UUID.randomUUID(), "X", 1))))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.delete().uri("/api/v1/log-entries/{id}?version=1", entry)
                .header(HttpHeaders.AUTHORIZATION, bearer(intruder))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(owner, "/api/v1/log-entries/{id}", entry)).bodyJson()
                .extractingPath("$.version").isEqualTo(1);
    }
}
