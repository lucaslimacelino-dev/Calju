package io.github.lucaslimacelino_dev.calju.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class UsdaB12Test {

    private static final Path OFFICIAL = Path.of("..", "..", "data", "fontes", "taco", "taco-4a-edicao-2011.xlsx");
    private static final Path CURATED = Path.of("..", "..", "data", "fontes", "usda", "b12-alimentos-comuns.csv");

    private static CatalogFood food(int number, String name) {
        return new CatalogFood(TacoParser.idFor(number), number, name, "Categoria",
                List.of(new NutrientValue("ENERC", BigDecimal.TEN, false)));
    }

    private static Map<String, String> row(String number, String name, String b12) {
        return Map.of("taco_number", number, "taco_name", name, "b12_ug_per_100g", b12);
    }

    private static NutrientValue b12(CatalogFood food) {
        return food.nutrients().stream().filter(v -> v.nutrientCode().equals("VITB12")).findFirst().orElse(null);
    }

    @Test
    void acrescentaB12SoNosAlimentosDaCuradoria() {
        List<CatalogFood> foods = UsdaB12.addTo(List.of(food(1, "Ovo"), food(2, "Pão"), food(3, "Pastel")),
                List.of(row("1", "Ovo", "1.11"), row("2", "Pão", "0")));

        assertEquals(new BigDecimal("1.1100"), b12(foods.get(0)).amount());
        assertEquals(new BigDecimal("0.0000"), b12(foods.get(1)).amount());
        assertNull(b12(foods.get(2)));
        assertEquals(2, foods.get(0).nutrients().size());
    }

    @Test
    void nomeDiferenteDoCatalogoInterrompe() {
        assertThrows(IllegalStateException.class,
                () -> UsdaB12.addTo(List.of(food(1, "Ovo")), List.of(row("1", "Ovo frito", "1"))));
    }

    @Test
    void alimentoInexistenteOuRepetidoInterrompe() {
        assertThrows(IllegalStateException.class,
                () -> UsdaB12.addTo(List.of(food(1, "Ovo")), List.of(row("9", "Ovo", "1"))));
        assertThrows(IllegalStateException.class,
                () -> UsdaB12.addTo(List.of(food(1, "Ovo")), List.of(row("1", "Ovo", "1"), row("1", "Ovo", "2"))));
    }

    @Test
    void curadoriaGuardadaConfereComAPlanilhaOficial() throws IOException {
        List<CatalogFood> foods = UsdaB12.addTo(
                TacoParser.parse(XlsxReader.readSheet(OFFICIAL, TacoParser.SHEET_NAME)), CsvReader.read(CURATED));

        CatalogFood ovo = foods.stream().filter(f -> f.sourceNumber() == 488).findFirst().orElseThrow();
        assertEquals(new BigDecimal("1.1100"), b12(ovo).amount());
    }
}
