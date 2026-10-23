package io.github.lucaslimacelino_dev.calju.food;

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

class FoodApiTest extends ApiTest {

    // Valores fictícios: só o comportamento importa.
    private static final String GRANOLA = """
            {"name":" Granola da casa ","brand":"Marca Teste",
             "nutrients":[{"code":"ENERC","amount":400,"qualifier":"VALUE"},
                          {"code":"PROCNT","amount":10,"qualifier":"VALUE"},
                          {"code":"NA","amount":null,"qualifier":"TRACE"}],
             "portion":{"label":"1 xícara","grams":40},"version":%d}
            """;

    private static Predicate<BigDecimal> same(String expected) {
        return value -> value.compareTo(new BigDecimal(expected)) == 0;
    }

    private MvcTestResult put(UUID user, UUID food, String json) {
        return mvc.put().uri("/api/v1/foods/{id}", food)
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    private MvcTestResult get(UUID user, String uri, Object... vars) {
        return mvc.get().uri(uri, vars).header(HttpHeaders.AUTHORIZATION, bearer(user)).exchange();
    }

    private MvcTestResult delete(UUID user, UUID food, int version) {
        return mvc.delete().uri("/api/v1/foods/{id}?version={v}", food, version)
                .header(HttpHeaders.AUTHORIZATION, bearer(user)).exchange();
    }

    @Test
    void criarGuardaNutrientesTracoEPorcao() {
        UUID user = newUser();
        UUID food = UUID.randomUUID();

        MvcTestResult result = put(user, food, GRANOLA.formatted(0));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Granola da casa");
        assertThat(result).bodyJson().extractingPath("$.nutrients.length()").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath("$.nutrients[?(@.code=='NA')].qualifier")
                .asArray().containsExactly("TRACE");
        assertThat(result).bodyJson().extractingPath("$.portion.label").isEqualTo("1 xícara");
        assertThat(get(user, "/api/v1/foods/{id}", food)).bodyJson()
                .extractingPath("$.portion.grams").convertTo(BigDecimal.class).matches(same("40"));
    }

    @Test
    void unidadePadraoEGramaEMlEGuardada() {
        UUID user = newUser();
        UUID granola = UUID.randomUUID();
        UUID suco = UUID.randomUUID();

        assertThat(put(user, granola, GRANOLA.formatted(0))).bodyJson().extractingPath("$.unit").isEqualTo("g");
        MvcTestResult result = put(user, suco, """
                {"name":"Suco de caju","brand":null,"unit":"ml",
                 "nutrients":[{"code":"ENERC","amount":45,"qualifier":"VALUE"}],
                 "portion":{"label":"1 copo","grams":200},"version":0}
                """);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(get(user, "/api/v1/foods/{id}", suco)).bodyJson().extractingPath("$.unit").isEqualTo("ml");
    }

    @Test
    void unidadeDesconhecidaERecusada() {
        MvcTestResult result = put(newUser(), UUID.randomUUID(), """
                {"name":"Suco","unit":"kg","nutrients":[],"portion":null,"version":0}
                """);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void atualizarSubstituiOsNutrientesEOQueSaiFicaDesconhecido() {
        UUID user = newUser();
        UUID food = UUID.randomUUID();
        put(user, food, GRANOLA.formatted(0));

        MvcTestResult result = put(user, food, """
                {"name":"Granola","brand":null,
                 "nutrients":[{"code":"ENERC","amount":380,"qualifier":"VALUE"}],
                 "portion":null,"version":1}
                """);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.nutrients.length()").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.portion").isNull();
    }

    @Test
    void nutrienteDesconhecidoOuRepetidoDa400() {
        UUID user = newUser();
        String unknown = """
                {"name":"X","nutrients":[{"code":"XYZ","amount":1,"qualifier":"VALUE"}],"version":0}
                """;
        String repeated = """
                {"name":"X","nutrients":[{"code":"FAT","amount":1,"qualifier":"VALUE"},
                                         {"code":"FAT","amount":2,"qualifier":"VALUE"}],"version":0}
                """;

        assertThat(put(user, UUID.randomUUID(), unknown)).hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().extractingPath("$.detail").isEqualTo("Nutriente desconhecido: XYZ.");
        assertThat(put(user, UUID.randomUUID(), repeated)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void valueSemQuantidadeOuTracoComQuantidadeDa400() {
        UUID user = newUser();
        String valueWithoutAmount = """
                {"name":"X","nutrients":[{"code":"FAT","amount":null,"qualifier":"VALUE"}],"version":0}
                """;
        String traceWithAmount = """
                {"name":"X","nutrients":[{"code":"FAT","amount":1,"qualifier":"TRACE"}],"version":0}
                """;

        assertThat(put(user, UUID.randomUUID(), valueWithoutAmount)).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(put(user, UUID.randomUUID(), traceWithAmount)).hasStatus(HttpStatus.BAD_REQUEST);
    }

    @Test
    void excluirTiraDaListaMasOIdAindaMostraEPutRestaura() {
        UUID user = newUser();
        UUID food = UUID.randomUUID();
        put(user, food, GRANOLA.formatted(0));

        MvcTestResult deleted = delete(user, food, 1);

        assertThat(deleted).hasStatus(HttpStatus.OK);
        assertThat(deleted).bodyJson().extractingPath("$.version").isEqualTo(2);
        assertThat(deleted).bodyJson().extractingPath("$.deletedAt").isNotNull();
        assertThat(get(user, "/api/v1/foods")).bodyJson().extractingPath("$.length()").isEqualTo(0);

        assertThat(put(user, food, GRANOLA.formatted(2))).hasStatus(HttpStatus.OK)
                .bodyJson().extractingPath("$.deletedAt").isNull();
        assertThat(get(user, "/api/v1/foods")).bodyJson().extractingPath("$.length()").isEqualTo(1);
    }

    @Test
    void excluirComVersaoDesatualizadaDa409() {
        UUID user = newUser();
        UUID food = UUID.randomUUID();
        put(user, food, GRANOLA.formatted(0));
        put(user, food, GRANOLA.formatted(1));

        assertThat(delete(user, food, 1)).hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.currentVersion").isEqualTo(2);
    }

    @Test
    void alimentoDeOutroUsuarioNaoEhLidoAlteradoNemExcluido() {
        UUID owner = newUser();
        UUID intruder = newUser();
        UUID food = UUID.randomUUID();
        put(owner, food, GRANOLA.formatted(0));

        assertThat(get(intruder, "/api/v1/foods/{id}", food)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(put(intruder, food, GRANOLA.formatted(1))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(delete(intruder, food, 1)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(get(owner, "/api/v1/foods/{id}", food)).bodyJson()
                .extractingPath("$.version").isEqualTo(1);
    }
}
