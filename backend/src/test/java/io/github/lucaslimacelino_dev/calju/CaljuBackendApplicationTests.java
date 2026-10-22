package io.github.lucaslimacelino_dev.calju;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

class CaljuBackendApplicationTests extends ApiTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void healthRespondeUp() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatus(HttpStatus.OK)
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("UP");
    }

    @Test
    void flywayAplicaAsMigracoes() {
        Integer falhas = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where not success", Integer.class);
        Integer aplicadas = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success", Integer.class);
        assertThat(falhas).isZero();
        assertThat(aplicadas).isGreaterThanOrEqualTo(2);
    }

    @Test
    void endpointsDoActuatorAlemDoHealthNaoSaoExpostosNemComToken() {
        assertThat(mvc.get().uri("/actuator/env")).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/actuator/env").header(HttpHeaders.AUTHORIZATION, bearer(newUser())))
                .hasStatus(HttpStatus.NOT_FOUND);
    }
}
