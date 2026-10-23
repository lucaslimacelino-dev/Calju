package io.github.lucaslimacelino_dev.calju.diary.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.github.lucaslimacelino_dev.calju.diary.LogItemEntity;
import io.github.lucaslimacelino_dev.calju.food.dto.NutrientValueDto;

/**
 * Item com a cópia do alimento: nome, marca, fonte, porção e nutrientes por 100 g. {@code unit}
 * ("g" ou "ml", ausente = "g") só muda como a quantidade é mostrada.
 */
public record LogItemDto(
        @NotNull UUID id,
        UUID foodId,
        @NotBlank @Size(max = 200) String foodName,
        @Size(max = 200) String brand,
        @Size(max = 50) String source,
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 8, fraction = 4) BigDecimal quantityG,
        @Pattern(regexp = "g|ml") String unit,
        @Size(max = 100) String portionLabel,
        @NotNull @Size(max = 100) List<@Valid @NotNull NutrientValueDto> nutrients) {

    public static LogItemDto from(LogItemEntity item) {
        return new LogItemDto(item.getId(), item.getFoodId(), item.getFoodName(), item.getBrand(), item.getSource(),
                item.getQuantityG(), item.getUnit(), item.getPortionLabel(),
                item.getNutrients().stream().map(NutrientValueDto::from).toList());
    }
}
