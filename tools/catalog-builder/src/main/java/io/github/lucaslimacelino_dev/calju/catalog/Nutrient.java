package io.github.lucaslimacelino_dev.calju.catalog;

/** Nutriente do catálogo: código INFOODS, nome exibido, unidade e ordem na tela. */
public record Nutrient(String code, String namePt, String unit, int displayOrder) {
}
