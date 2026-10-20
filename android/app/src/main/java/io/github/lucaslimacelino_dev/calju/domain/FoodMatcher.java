package io.github.lucaslimacelino_dev.calju.domain;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Busca de alimentos por nome, sem diferenciar acentos, maiúsculas e plural simples:
 * "maca" encontra "Maçã" e "ovos" encontra "Ovo". Todas as palavras digitadas precisam aparecer.
 */
public final class FoodMatcher {

    private FoodMatcher() {
    }

    public static String normalize(String text) {
        Objects.requireNonNull(text, "text");
        String withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    public static List<String> terms(String query) {
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> terms = new ArrayList<>();
        for (String term : normalized.split(" ")) {
            terms.add(singular(term));
        }
        return terms;
    }

    /** {@code normalizedName} deve vir de {@link #normalize(String)}. */
    public static boolean matches(String normalizedName, List<String> terms) {
        if (terms.isEmpty()) {
            return false;
        }
        for (String term : terms) {
            if (!normalizedName.contains(term)) {
                return false;
            }
        }
        return true;
    }

    /** Menor é melhor: nome que começa com a primeira palavra vem antes; depois, nomes mais curtos. */
    public static int score(String normalizedName, List<String> terms) {
        int startsWith = normalizedName.startsWith(terms.get(0)) ? 0 : 1000;
        return startsWith + normalizedName.length();
    }

    private static String singular(String term) {
        if (term.length() >= 4 && (term.endsWith("oes") || term.endsWith("aes"))) {
            return term.substring(0, term.length() - 3) + "ao";
        }
        if (term.length() > 3 && term.endsWith("s")) {
            return term.substring(0, term.length() - 1);
        }
        return term;
    }
}
