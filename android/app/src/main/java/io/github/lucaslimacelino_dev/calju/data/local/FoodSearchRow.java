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
    public String brand;

    @Nullable
    public String portionLabel;

    @Nullable
    public BigDecimal portionGrams;

    /** Nome do dia a dia, quando o alimento está na lista de comuns. */
    @Nullable
    public String displayName;

    public boolean common;

    public boolean custom;

    public FoodSearchRow(@NonNull String id, @NonNull String name, @NonNull String searchName,
                         @Nullable String category, @Nullable String brand, @Nullable String portionLabel,
                         @Nullable BigDecimal portionGrams, @Nullable String displayName, boolean common,
                         boolean custom) {
        this.id = id;
        this.name = name;
        this.searchName = searchName;
        this.category = category;
        this.brand = brand;
        this.portionLabel = portionLabel;
        this.portionGrams = portionGrams;
        this.displayName = displayName;
        this.common = common;
        this.custom = custom;
    }

    /** O nome a mostrar: o do dia a dia, se houver. */
    public String shownName() {
        return displayName != null ? displayName : name;
    }

    /** Comuns e criados pelo usuário aparecem primeiro; o resto fica em "Ver mais opções". */
    public boolean featured() {
        return common || custom;
    }
}
