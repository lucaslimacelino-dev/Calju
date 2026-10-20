package io.github.lucaslimacelino_dev.calju.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.Arrays;

import org.junit.Test;

public class CsvLineTest {

    @Test
    public void camposSimples() {
        assertEquals(Arrays.asList("a", "1", ""), CsvLine.parse("a,1,"));
    }

    @Test
    public void aspasProtegemVirgulas() {
        assertEquals(Arrays.asList("id", "Arroz, tipo 1, cozido", "Cereais"),
                CsvLine.parse("id,\"Arroz, tipo 1, cozido\",Cereais"));
    }

    @Test
    public void aspasDuplicadasViramUmaAspa() {
        assertEquals(Arrays.asList("Queijo \"minas\""), CsvLine.parse("\"Queijo \"\"minas\"\"\""));
    }

    @Test
    public void aspasNaoFechadasSaoErro() {
        assertThrows(IllegalArgumentException.class, () -> CsvLine.parse("\"aberta,1"));
    }
}
