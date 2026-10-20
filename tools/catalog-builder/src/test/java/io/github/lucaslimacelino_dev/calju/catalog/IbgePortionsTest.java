package io.github.lucaslimacelino_dev.calju.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class IbgePortionsTest {

    private static final CatalogFood FRUTA = new CatalogFood(TacoParser.idFor(1), 1, "Fruta A, crua", "Frutas", List.of());

    private static Map<String, String> row(String number, String name, String grams) {
        return Map.of("taco_number", number, "taco_name", name, "label", "1 unidade", "grams", grams);
    }

    @Test
    void ligaPorcaoAoAlimentoPeloNumeroDaTaco() {
        List<Portion> portions = IbgePortions.link(List.of(row("1", "Fruta A, crua", "75")), List.of(FRUTA));

        assertEquals(FRUTA.id(), portions.get(0).foodId());
        assertEquals(new BigDecimal("75"), portions.get(0).grams());
        assertEquals(IbgePortions.SOURCE_ID, portions.get(0).sourceId());
    }

    @Test
    void nomeDiferenteDoCatalogoInterrompeAGeracao() {
        assertThrows(IllegalStateException.class,
                () -> IbgePortions.link(List.of(row("1", "Outro nome", "75")), List.of(FRUTA)));
    }

    @Test
    void alimentoAusenteInterrompeAGeracao() {
        assertThrows(IllegalStateException.class,
                () -> IbgePortions.link(List.of(row("2", "Fruta A, crua", "75")), List.of(FRUTA)));
    }

    @Test
    void pesoZeroInterrompeAGeracao() {
        assertThrows(IllegalStateException.class,
                () -> IbgePortions.link(List.of(row("1", "Fruta A, crua", "0")), List.of(FRUTA)));
    }

    @Test
    void curadoriaOficialBateComOCatalogoDaTaco() throws IOException {
        Path taco = Path.of("..", "..", "data", "fontes", "taco", "taco-4a-edicao-2011.xlsx");
        Path curated = Path.of("..", "..", "data", "fontes", "ibge", "porcoes-unidade.csv");
        List<CatalogFood> foods = TacoParser.parse(XlsxReader.readSheet(taco, TacoParser.SHEET_NAME));

        List<Portion> portions = IbgePortions.link(CsvReader.read(curated), foods);

        assertEquals(CsvReader.read(curated).size(), portions.size());
    }
}
