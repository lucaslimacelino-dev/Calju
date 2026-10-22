package io.github.lucaslimacelino_dev.calju.goal;

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

class GoalApiTest extends ApiTest {

    private static String body(String effectiveFrom, String kcal, String protein, int version) {
        return """
                {"effectiveFrom":"%s","kcal":%s,"proteinG":%s,"carbohydrateG":null,"fatG":null,"version":%d}
                """.formatted(effectiveFrom, kcal, protein, version);
    }

    /** Compara pelo valor: o banco devolve 1900.0000 e o pedido mandou 1900. */
    private static Predicate<BigDecimal> same(String expected) {
        return value -> value.compareTo(new BigDecimal(expected)) == 0;
    }

    private MvcTestResult put(UUID user, UUID goalId, String json) {
        return mvc.put().uri("/api/v1/goals/{id}", goalId)
                .header(HttpHeaders.AUTHORIZATION, bearer(user))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    @Test
    void criarComVersaoZeroDevolve201ComVersao1() {
        UUID user = newUser();
        UUID goal = UUID.randomUUID();

        MvcTestResult result = put(user, goal, body("2026-10-22", "2000", "90", 0));

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(1);
        assertThat(result).bodyJson().extractingPath("$.kcal").convertTo(BigDecimal.class).matches(same("2000"));
        assertThat(mvc.get().uri("/api/v1/goals").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .bodyJson().extractingPath("$[0].id").isEqualTo(goal.toString());
    }

    @Test
    void atualizarComVersaoAtualAvancaAVersao() {
        UUID user = newUser();
        UUID goal = UUID.randomUUID();
        put(user, goal, body("2026-10-22", "2000", "90", 0));

        MvcTestResult result = put(user, goal, body("2026-10-22", "1800", "90", 1));

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.kcal").convertTo(BigDecimal.class).matches(same("1800"));
    }

    @Test
    void versaoDesatualizadaDa409ComAVersaoAtualENaoAltera() {
        UUID user = newUser();
        UUID goal = UUID.randomUUID();
        put(user, goal, body("2026-10-22", "2000", "90", 0));
        put(user, goal, body("2026-10-22", "1900", "90", 1));

        MvcTestResult stale = put(user, goal, body("2026-10-22", "1500", "90", 1));

        assertThat(stale).hasStatus(HttpStatus.CONFLICT);
        assertThat(stale).bodyJson().extractingPath("$.currentVersion").isEqualTo(2);
        assertThat(mvc.get().uri("/api/v1/goals/{id}", goal).header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .bodyJson().extractingPath("$.kcal").convertTo(BigDecimal.class).matches(same("1900"));
    }

    @Test
    void repetirACriacaoNaoDuplica() {
        UUID user = newUser();
        UUID goal = UUID.randomUUID();
        put(user, goal, body("2026-10-22", "2000", "90", 0));

        assertThat(put(user, goal, body("2026-10-22", "2000", "90", 0))).hasStatus(HttpStatus.CONFLICT);
        assertThat(mvc.get().uri("/api/v1/goals").header(HttpHeaders.AUTHORIZATION, bearer(user)))
                .bodyJson().extractingPath("$.length()").isEqualTo(1);
    }

    @Test
    void duasMetasNoMesmoDiaDao409() {
        UUID user = newUser();
        put(user, UUID.randomUUID(), body("2026-10-22", "2000", "90", 0));

        assertThat(put(user, UUID.randomUUID(), body("2026-10-22", "1500", "80", 0)))
                .hasStatus(HttpStatus.CONFLICT);
    }

    @Test
    void metaDeOutroUsuarioRespondeComo404ENaoMuda() {
        UUID owner = newUser();
        UUID intruder = newUser();
        UUID goal = UUID.randomUUID();
        put(owner, goal, body("2026-10-22", "2000", "90", 0));

        assertThat(mvc.get().uri("/api/v1/goals/{id}", goal).header(HttpHeaders.AUTHORIZATION, bearer(intruder)))
                .hasStatus(HttpStatus.NOT_FOUND);
        assertThat(put(intruder, goal, body("2026-10-22", "1", "1", 1))).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mvc.get().uri("/api/v1/goals").header(HttpHeaders.AUTHORIZATION, bearer(intruder)))
                .bodyJson().extractingPath("$.length()").isEqualTo(0);
        assertThat(mvc.get().uri("/api/v1/goals/{id}", goal).header(HttpHeaders.AUTHORIZATION, bearer(owner)))
                .bodyJson().extractingPath("$.kcal").convertTo(BigDecimal.class).matches(same("2000"));
    }

    @Test
    void dadosInvalidosDao400ComOsCampos() {
        MvcTestResult result = put(newUser(), UUID.randomUUID(), body("2026-10-22", "0", "-5", 0));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.errors[*].field")
                .asArray().containsExactlyInAnyOrder("kcal", "proteinG");
    }

    @Test
    void jsonMalformadoDa400SemDetalhesInternos() {
        MvcTestResult result = put(newUser(), UUID.randomUUID(), "{\"kcal\":");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST)
                .hasContentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyText().doesNotContain("Exception").doesNotContain("at io.github");
    }
}
