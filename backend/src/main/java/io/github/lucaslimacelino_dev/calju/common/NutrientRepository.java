package io.github.lucaslimacelino_dev.calju.common;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

/** Lista fixa de nutrientes (tabela nutrients, criada pela migração V3). Só leitura. */
public interface NutrientRepository extends Repository<NutrientRow, String> {

    @Query("select n.code from NutrientRow n")
    List<String> allCodes();
}
