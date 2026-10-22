package io.github.lucaslimacelino_dev.calju.common;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

/** Confere códigos de nutriente: existentes na tabela nutrients e sem repetição. */
@Component
public class NutrientValidator {

    private final NutrientRepository nutrients;
    private volatile Set<String> known;

    public NutrientValidator(NutrientRepository nutrients) {
        this.nutrients = nutrients;
    }

    public void requireValid(List<String> codes) {
        Set<String> valid = known();
        Set<String> seen = new HashSet<>();
        for (String code : codes) {
            if (!valid.contains(code)) {
                throw new BadRequestException("Nutriente desconhecido: " + code + ".");
            }
            if (!seen.add(code)) {
                throw new BadRequestException("Nutriente repetido: " + code + ".");
            }
        }
    }

    /** A lista só muda com uma migração nova, então é lida do banco uma vez. */
    private Set<String> known() {
        Set<String> current = known;
        if (current == null) {
            current = Set.copyOf(nutrients.allCodes());
            known = current;
        }
        return current;
    }
}
