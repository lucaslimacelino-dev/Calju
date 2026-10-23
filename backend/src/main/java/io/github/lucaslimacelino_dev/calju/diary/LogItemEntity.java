package io.github.lucaslimacelino_dev.calju.diary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import io.github.lucaslimacelino_dev.calju.common.NutrientAmount;

/** Item de um registro, com a cópia do nome e dos nutrientes (por 100 g) de quando foi consumido. */
@Entity
@Table(name = "log_items")
public class LogItemEntity {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "entry_id", nullable = false, updatable = false)
    private LogEntryEntity entry;

    @Column(name = "food_id")
    private UUID foodId;

    @Column(name = "food_name_snap", nullable = false, length = 200)
    private String foodName;

    @Column(name = "brand_snap", length = 200)
    private String brand;

    @Column(name = "source_snap", length = 50)
    private String source;

    @Column(name = "quantity_g", nullable = false, precision = 12, scale = 4)
    private BigDecimal quantityG;

    @Column(nullable = false, length = 2)
    private String unit = "g";

    @Column(name = "portion_label_snap", length = 100)
    private String portionLabel;

    @Column(nullable = false)
    private int position;

    @ElementCollection
    @CollectionTable(name = "log_item_nutrients", joinColumns = @JoinColumn(name = "log_item_id"))
    private List<NutrientAmount> nutrients = new ArrayList<>();

    protected LogItemEntity() {
    }

    LogItemEntity(UUID id, LogEntryEntity entry) {
        this.id = id;
        this.entry = entry;
    }

    void apply(UUID foodId, String foodName, String brand, String source, BigDecimal quantityG, String unit,
               String portionLabel, int position, List<NutrientAmount> nutrients) {
        this.foodId = foodId;
        this.foodName = foodName;
        this.brand = brand;
        this.source = source;
        this.quantityG = quantityG;
        this.unit = unit;
        this.portionLabel = portionLabel;
        this.position = position;
        this.nutrients.clear();
        this.nutrients.addAll(nutrients);
    }

    public UUID getId() {
        return id;
    }

    public UUID getFoodId() {
        return foodId;
    }

    public String getFoodName() {
        return foodName;
    }

    public String getBrand() {
        return brand;
    }

    public String getSource() {
        return source;
    }

    public BigDecimal getQuantityG() {
        return quantityG;
    }

    public String getUnit() {
        return unit;
    }

    public String getPortionLabel() {
        return portionLabel;
    }

    public int getPosition() {
        return position;
    }

    public List<NutrientAmount> getNutrients() {
        return nutrients;
    }
}
