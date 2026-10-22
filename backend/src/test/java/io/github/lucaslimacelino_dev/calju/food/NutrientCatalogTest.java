package io.github.lucaslimacelino_dev.calju.food;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import io.github.lucaslimacelino_dev.calju.ApiTest;

/** A lista de nutrientes do servidor (migração V3) tem de ser a mesma do catálogo do app. */
class NutrientCatalogTest extends ApiTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void codigosDoServidorSaoOsMesmosDoCatalogoDoApp() throws Exception {
        List<String> lines = Files.readAllLines(Path.of("../data/catalog/nutrients.csv"), StandardCharsets.UTF_8);
        List<String> appCodes = lines.stream().skip(1).filter(line -> !line.isBlank())
                .map(line -> line.substring(0, line.indexOf(','))).toList();

        List<String> serverCodes = jdbc.queryForList("select code from nutrients", String.class);

        assertThat(serverCodes).containsExactlyInAnyOrderElementsOf(appCodes);
    }
}
