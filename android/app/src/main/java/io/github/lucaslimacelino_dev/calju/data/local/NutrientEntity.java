package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "nutrients")
public class NutrientEntity {

    @PrimaryKey
    @NonNull
    public String code;

    @ColumnInfo(name = "name_pt")
    @NonNull
    public String namePt;

    @NonNull
    public String unit;

    @ColumnInfo(name = "display_order")
    public int displayOrder;

    public NutrientEntity(@NonNull String code, @NonNull String namePt, @NonNull String unit, int displayOrder) {
        this.code = code;
        this.namePt = namePt;
        this.unit = unit;
        this.displayOrder = displayOrder;
    }
}
