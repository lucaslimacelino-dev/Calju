package io.github.lucaslimacelino_dev.calju.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

class TacoParserTest {

    private static final String[][] HEADER = {
            {"C", "Umidade", "(%)"}, {"D", "Energia", "(kcal)"}, {"F", "Proteína", "(g)"},
            {"G", "Lipídeos", "(g)"}, {"H", "Colesterol", "(mg)"}, {"I", "idrato", "(g)"},
            {"J", "Alimentar", "(g)"}, {"K", "Cinzas", "(g)"}, {"L", "Cálcio", "(mg)"},
            {"M", "Magnésio", "(mg)"}, {"O", "Manganês", "(mg)"}, {"P", "Fósforo", "(mg)"},
            {"Q", "Ferro", "(mg)"}, {"R", "Sódio", "(mg)"}, {"S", "Potássio", "(mg)"},
            {"T", "Cobre", "(mg)"}, {"U", "Zinco", "(mg)"}, {"V", "Retinol", "(mcg)"},
            {"W", "RE", "(mcg)"}, {"Y", "Tiamina", "(mg)"}, {"Z", "Riboflavina", "(mg)"},
            {"AA", "Piridoxina", "(mg)"}, {"AB", "Niacina", "(mg)"}, {"AC", "C", "(mg)"}};

    private final Map<Integer, Map<String, String>> sheet = new TreeMap<>();
    private int nextRow = 1;

    private void row(String... columnAndValue) {
        Map<String, String> cells = new HashMap<>();
        for (int i = 0; i < columnAndValue.length; i += 2) {
            cells.put(columnAndValue[i], columnAndValue[i + 1]);
        }
        sheet.put(nextRow++, cells);
    }

    private void header() {
        Map<String, String> names = new HashMap<>(Map.of("A", "Número do"));
        Map<String, String> units = new HashMap<>(Map.of("A", "Alimento", "B", "Descrição dos alimentos"));
        for (String[] column : HEADER) {
            names.put(column[0], column[1]);
            units.put(column[0], column[2]);
        }
        sheet.put(nextRow++, names);
        sheet.put(nextRow++, units);
    }

    private static NutrientValue value(CatalogFood food, String code) {
        return food.nutrients().stream().filter(v -> v.nutrientCode().equals(code)).findFirst().orElse(null);
    }

    @Test
    void leAlimentoComCategoriaETratamentoDosMarcadores() {
        header();
        row("A", "Categoria X");
        row("A", "1", "B", "Alimento  teste,  cru", "D", "123.456789", "F", "Tr", "G", "NA", "H", "*", "I", " ");

        List<CatalogFood> foods = TacoParser.parse(sheet);

        assertEquals(1, foods.size());
        CatalogFood food = foods.get(0);
        assertEquals("Alimento teste, cru", food.name());
        assertEquals("Categoria X", food.category());
        assertEquals(new BigDecimal("123.4568"), value(food, "ENERC").amount());
        assertTrue(value(food, "PROCNT").trace());
        assertNull(value(food, "FAT"), "NA vira desconhecido");
        assertNull(value(food, "CHOLE"), "* vira desconhecido");
        assertNull(value(food, "CHOCDF"), "vazio vira desconhecido");
    }

    @Test
    void aceitaNotacaoCientificaEArredondaParaQuatroCasas() {
        header();
        row("A", "Categoria X");
        row("A", "1", "B", "Alimento A", "T", "2.0333333333333332E-2");

        assertEquals(new BigDecimal("0.0203"), value(TacoParser.parse(sheet).get(0), "CU").amount());
    }

    @Test
    void ignoraCabecalhoRepetidoEParaNaLegenda() {
        header();
        row("A", "Categoria X");
        row("A", "1", "B", "Alimento A", "D", "10");
        header();
        row("A", "2", "B", "Alimento B", "D", "20");
        row("A", "Legenda");
        row("A", "3", "B", "Não é alimento", "D", "30");

        List<CatalogFood> foods = TacoParser.parse(sheet);

        assertEquals(List.of("Alimento A", "Alimento B"), foods.stream().map(CatalogFood::name).toList());
        assertEquals("Categoria X", foods.get(1).category());
    }

    @Test
    void alimentoSemNenhumValorConhecidoFicaDeFora() {
        header();
        row("A", "Categoria X");
        row("A", "1", "B", "Em reavaliação", "C", "*", "D", "*");

        assertTrue(TacoParser.parse(sheet).isEmpty());
    }

    @Test
    void valorNaoReconhecidoInterrompeAGeracao() {
        header();
        row("A", "Categoria X");
        row("A", "1", "B", "Alimento A", "D", "abc");

        IllegalStateException error = assertThrows(IllegalStateException.class, () -> TacoParser.parse(sheet));
        assertTrue(error.getMessage().contains("D"));
    }

    @Test
    void valorNegativoInterrompeAGeracao() {
        header();
        row("A", "Categoria X");
        row("A", "1", "B", "Alimento A", "D", "-1");

        assertThrows(IllegalStateException.class, () -> TacoParser.parse(sheet));
    }

    @Test
    void carboidratoNegativoViraZero() {
        header();
        row("A", "Categoria X");
        row("A", "1", "B", "Alimento A", "I", "-2.6666666666666172E-2");

        assertEquals(new BigDecimal("0.0000"), value(TacoParser.parse(sheet).get(0), "CHOCDF").amount());
    }

    @Test
    void colunaForaDoLugarInterrompeAGeracao() {
        header();
        sheet.get(1).put("D", "Proteína");

        assertThrows(IllegalStateException.class, () -> TacoParser.parse(sheet));
    }

    @Test
    void unidadeDiferenteInterrompeAGeracao() {
        header();
        sheet.get(2).put("D", "(kJ)");

        assertThrows(IllegalStateException.class, () -> TacoParser.parse(sheet));
    }

    @Test
    void identificadorEhEstavelEntreExecucoes() {
        assertEquals(TacoParser.idFor(42), TacoParser.idFor(42));
        assertTrue(!TacoParser.idFor(42).equals(TacoParser.idFor(43)));
    }
}
