package io.github.lucaslimacelino_dev.calju.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class FoodMatcherTest {

    private static boolean finds(String query, String foodName) {
        return FoodMatcher.matches(FoodMatcher.normalize(foodName), FoodMatcher.terms(query));
    }

    @Test
    public void normalizaAcentosMaiusculasEPontuacao() {
        assertEquals("maca fuji com casca crua", FoodMatcher.normalize("Maçã, Fuji, com casca, crua"));
        assertEquals("pao de queijo assado", FoodMatcher.normalize("  Pão,  de queijo, assado "));
    }

    @Test
    public void ignoraAcentos() {
        assertTrue(finds("maca", "Maçã, Fuji, com casca, crua"));
        assertTrue(finds("MAÇÃ", "Maçã, Fuji, com casca, crua"));
    }

    @Test
    public void todasAsPalavrasPrecisamAparecerEmQualquerOrdem() {
        assertTrue(finds("arroz cozido", "Arroz, tipo 1, cozido"));
        assertTrue(finds("cozido arroz", "Arroz, tipo 1, cozido"));
        assertFalse(finds("arroz cru", "Arroz, tipo 1, cozido"));
    }

    @Test
    public void aceitaPluralSimples() {
        assertTrue(finds("ovos", "Ovo, de galinha, inteiro, cru"));
        assertTrue(finds("bananas", "Banana, prata, crua"));
        assertTrue(finds("pães", "Pão, trigo, francês"));
        assertTrue(finds("limões", "Limão, tahiti, cru"));
    }

    @Test
    public void palavraParcialEncontra() {
        assertTrue(finds("ban", "Banana, prata, crua"));
    }

    @Test
    public void buscaVaziaNaoEncontraNada() {
        assertFalse(finds("   ", "Banana, prata, crua"));
        assertEquals(Collections.emptyList(), FoodMatcher.terms(""));
    }

    @Test
    public void nomeQueComecaComAPalavraVemPrimeiro() {
        List<String> terms = FoodMatcher.terms("banana");
        int comeca = FoodMatcher.score(FoodMatcher.normalize("Banana, prata, crua"), terms);
        int contem = FoodMatcher.score(FoodMatcher.normalize("Doce, de banana"), terms);
        assertTrue(comeca < contem);
    }

    @Test
    public void entreOsQueComecamONomeMaisCurtoVemPrimeiro() {
        List<String> terms = Arrays.asList("ovo");
        assertTrue(FoodMatcher.score("ovo de galinha inteiro cru", terms)
                < FoodMatcher.score("ovo de galinha inteiro cozido 10minutos", terms));
    }
}
