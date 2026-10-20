package io.github.lucaslimacelino_dev.calju.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvWriterTest {

    @Test
    void textoSimplesFicaSemAspas() {
        assertEquals("Banana", CsvWriter.escape("Banana"));
    }

    @Test
    void virgulaEAspasSaoProtegidas() {
        assertEquals("\"Arroz, tipo 1, cozido\"", CsvWriter.escape("Arroz, tipo 1, cozido"));
        assertEquals("\"Queijo \"\"minas\"\"\"", CsvWriter.escape("Queijo \"minas\""));
    }

    @Test
    void gravaUtf8ComQuebraLf(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("saida.csv");

        CsvWriter.write(file, List.of("nome", "unidade"), List.of(List.of("Pão", "µg")));

        assertEquals("nome,unidade\nPão,µg\n", Files.readString(file, StandardCharsets.UTF_8));
    }
}
