package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;

/**
 * Item consumido, com cópia (snapshot) do nome e da fonte no momento do registro.
 * {@code foodId} é só uma referência: não há chave estrangeira, para que o registro antigo
 * sobreviva a mudanças no catálogo.
 */
@Entity(tableName = "log_items",
        foreignKeys = @ForeignKey(entity = LogEntryEntity.class, parentColumns = "id", childColumns = "entry_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("entry_id")})
public class LogItemEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "entry_id")
    @NonNull
    public String entryId;

    @ColumnInfo(name = "food_id")
    @Nullable
    public String foodId;

    @ColumnInfo(name = "food_name_snap")
    @NonNull
    public String foodNameSnap;

    @ColumnInfo(name = "brand_snap")
    @Nullable
    public String brandSnap;

    @ColumnInfo(name = "source_snap")
    @Nullable
    public String sourceSnap;

    @ColumnInfo(name = "quantity_g")
    @NonNull
    public BigDecimal quantityG;

    @ColumnInfo(name = "portion_label_snap")
    @Nullable
    public String portionLabelSnap;

    public int position;

    /** Unidade do alimento no momento do registro ("g" ou "ml"); a quantidade fica em quantity_g. */
    @ColumnInfo(defaultValue = "g")
    @NonNull
    public String unit = "g";

    public LogItemEntity(@NonNull String id, @NonNull String entryId, @Nullable String foodId,
                         @NonNull String foodNameSnap, @Nullable String brandSnap, @Nullable String sourceSnap,
                         @NonNull BigDecimal quantityG, @Nullable String portionLabelSnap, int position) {
        this.id = id;
        this.entryId = entryId;
        this.foodId = foodId;
        this.foodNameSnap = foodNameSnap;
        this.brandSnap = brandSnap;
        this.sourceSnap = sourceSnap;
        this.quantityG = quantityG;
        this.portionLabelSnap = portionLabelSnap;
        this.position = position;
    }
}
