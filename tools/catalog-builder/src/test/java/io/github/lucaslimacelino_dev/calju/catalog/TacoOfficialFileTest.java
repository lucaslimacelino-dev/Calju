package io.github.lucaslimacelino_dev.calju.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Lê a planilha oficial guardada em data/fontes e confere valores contra a própria planilha. */
class TacoOfficialFileTest {

    private static final Path OFFICIAL = Path.of("..", "..", "data", "fontes", "taco", "taco-4a-edicao-2011.xlsx");

    private static List<CatalogFood> foods;

    @BeforeAll
    static void readOfficialFile() throws IOException {
        foods = TacoParser.parse(XlsxReader.readSheet(OFFICIAL, TacoParser.SHEET_NAME));
    }

    private static CatalogFood byName(String name) {
        return foods.stream().filter(f -> f.name().equals(name)).findFirst().orElseThrow();
    }

    private static NutrientValue value(CatalogFood food, String code) {
        return food.nutrients().stream().filter(v -> v.nutrientCode().equals(code)).findFirst().orElse(null);
    }

    @Test
    void numerosDosAlimentosSaoUnicosEDentroDaFaixaDaTaco() {
        Set<Integer> numbers = foods.stream().map(CatalogFood::sourceNumber).collect(Collectors.toSet());
        assertEquals(foods.size(), numbers.size());
        assertTrue(numbers.stream().allMatch(n -> n >= 1 && n <= 597));
    }

    @Test
    void arrozTipo1CozidoConfereComAPlanilha() {
        CatalogFood arroz = byName("Arroz, tipo 1, cozido");

        assertEquals(3, arroz.sourceNumber());
        assertEquals("Cereais e derivados", arroz.category());
        assertEquals(new BigDecimal("128.2585"), value(arroz, "ENERC").amount());
        assertEquals(new BigDecimal("2.5208"), value(arroz, "PROCNT").amount());
        assertTrue(value(arroz, "THIA").trace());
        assertEquals(null, value(arroz, "CHOLE"), "NA na planilha");
    }

    @Test
    void carboidratoNegativoDaPlanilhaFicaZeroComoNaTabelaImpressa() {
        assertEquals(new BigDecimal("0.0000"), value(byName("Corimba, cru"), "CHOCDF").amount());
        assertEquals(new BigDecimal("0.0000"), value(byName("Frango, fígado, cru"), "CHOCDF").amount());
    }

    @Test
    void celulaComErroDeDigitacaoFicaDesconhecida() {
        CatalogFood paleta = byName("Carne, bovina, paleta, com gordura, crua");
        assertEquals(null, value(paleta, "VITB6A"));
        assertTrue(value(paleta, "ENERC") != null);
    }

    @Test
    void vitaminaAEmRaeVemDaColunaPropriaDaPlanilha() {
        CatalogFood cenoura = byName("Cenoura, crua");
        assertEquals(new BigDecimal("1326.0000"), value(cenoura, "VITA").amount());
        assertEquals(new BigDecimal("663.0000"), value(cenoura, "VITA_RAE").amount());
    }

    @Test
    void alimentoEmReavaliacaoFicaDeFora() {
        assertTrue(foods.stream().noneMatch(f -> f.name().equals("Coco, verde, cru")));
    }
}
