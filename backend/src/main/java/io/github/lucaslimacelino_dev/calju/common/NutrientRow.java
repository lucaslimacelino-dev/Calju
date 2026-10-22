package io.github.lucaslimacelino_dev.calju.common;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "nutrients")
public class NutrientRow {

    @Id
    @Column(length = 16)
    private String code;

    @Column(name = "name_pt", nullable = false, length = 100)
    private String namePt;

    @Column(nullable = false, length = 8)
    private String unit;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    protected NutrientRow() {
    }

    public String getCode() {
        return code;
    }
}
