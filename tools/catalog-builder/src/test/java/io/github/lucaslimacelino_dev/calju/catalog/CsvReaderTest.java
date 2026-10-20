package io.github.lucaslimacelino_dev.calju.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CsvReaderTest {

    @Test
    void leCamposComVirgulaEAspas() {
        List<List<String>> records = CsvReader.parse("a,b\n\"Arroz, cozido\",\"Queijo \"\"minas\"\"\"\n");

        assertEquals(List.of("Arroz, cozido", "Queijo \"minas\""), records.get(1));
    }

    @Test
    void aceitaQuebraCrlfEUltimaLinhaSemQuebra() {
        List<List<String>> records = CsvReader.parse("a,b\r\n1,2");

        assertEquals(List.of(List.of("a", "b"), List.of("1", "2")), records);
    }

    @Test
    void devolveMapaPorNomeDaColuna(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("x.csv");
        Files.writeString(file, "nome,peso\nPão,50\n", StandardCharsets.UTF_8);

        assertEquals(List.of(Map.of("nome", "Pão", "peso", "50")), CsvReader.read(file));
    }

    @Test
    void linhaComNumeroErradoDeColunasGeraErro(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("x.csv");
        Files.writeString(file, "a,b\n1,2,3\n", StandardCharsets.UTF_8);

        assertThrows(IOException.class, () -> CsvReader.read(file));
    }

    @Test
    void lerOQueOCsvWriterGravaDevolveOsMesmosValores(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("ida-e-volta.csv");
        CsvWriter.write(file, List.of("nome"), List.of(List.of("Laranja, \"pêra\", crua")));

        assertEquals("Laranja, \"pêra\", crua", CsvReader.read(file).get(0).get("nome"));
    }
}
