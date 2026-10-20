package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Linha única que guarda qual versão do catálogo já foi importada. */
@Entity(tableName = "catalog_info")
public class CatalogInfoEntity {

    public static final int SINGLE_ROW_ID = 1;

    @PrimaryKey
    public int id;

    public int version;

    public CatalogInfoEntity(int id, int version) {
        this.id = id;
        this.version = version;
    }
}
