package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.math.BigDecimal;

/** Dados mínimos de um alimento para a lista de sugestões, com a porção padrão, se houver. */
public class FoodSearchRow {

    @NonNull
    public String id;

    @NonNull
    public String name;

    @NonNull
    public String searchName;

    @Nullable
    public String category;

    @Nullable
    public String portionLabel;

    @Nullable
    public BigDecimal portionGrams;

    public FoodSearchRow(@NonNull String id, @NonNull String name, @NonNull String searchName,
                         @Nullable String category, @Nullable String portionLabel,
                         @Nullable BigDecimal portionGrams) {
        this.id = id;
        this.name = name;
        this.searchName = searchName;
        this.category = category;
        this.portionLabel = portionLabel;
        this.portionGrams = portionGrams;
    }
}
