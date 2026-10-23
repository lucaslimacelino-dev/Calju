package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/** Alimento do catálogo (TACO) ou criado pelo usuário ({@code custom}). */
@Entity(tableName = "foods")
public class FoodEntity {

    /** UUID em texto; os alimentos da TACO têm UUID fixo, gerado pelo catalog-builder. */
    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String name;

    /** Nome (e marca) sem acentos, minúsculo e sem pontuação, usado na busca. */
    @ColumnInfo(name = "search_name")
    @NonNull
    public String searchName;

    @Nullable
    public String category;

    @ColumnInfo(name = "source_id")
    @Nullable
    public String sourceId;

    @ColumnInfo(name = "source_number")
    @Nullable
    public Integer sourceNumber;

    @Nullable
    public String brand;

    @ColumnInfo(name = "is_custom", defaultValue = "0")
    public boolean custom;

    @ColumnInfo(name = "updated_at")
    @Nullable
    public Long updatedAt;

    /** Exclusão lógica (só alimentos do usuário): some da busca, mas moldes antigos ainda o leem. */
    @ColumnInfo(name = "deleted_at")
    @Nullable
    public Long deletedAt;

    @ColumnInfo(defaultValue = "1")
    public int version;

    /** Nulo para o catálogo oficial; "PENDING" para alimentos do usuário ainda não sincronizados. */
    @ColumnInfo(name = "sync_state")
    @Nullable
    public String syncState;

    /** Última versão confirmada pelo servidor; 0 = nunca enviado (ver SyncStates). */
    @ColumnInfo(name = "server_version", defaultValue = "0")
    public int serverVersion;

    /** "g" ou "ml" (ver QuantityUnit); os valores são por 100 da unidade. */
    @ColumnInfo(defaultValue = "g")
    @NonNull
    public String unit = "g";

    public FoodEntity(@NonNull String id, @NonNull String name, @NonNull String searchName,
                      @Nullable String category, @Nullable String sourceId, @Nullable Integer sourceNumber,
                      @Nullable String brand, boolean custom, @Nullable Long updatedAt, @Nullable Long deletedAt,
                      int version, @Nullable String syncState) {
        this.id = id;
        this.name = name;
        this.searchName = searchName;
        this.category = category;
        this.sourceId = sourceId;
        this.sourceNumber = sourceNumber;
        this.brand = brand;
        this.custom = custom;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
        this.syncState = syncState;
    }

    /** Alimento do catálogo oficial. */
    @Ignore
    public FoodEntity(@NonNull String id, @NonNull String name, @NonNull String searchName,
                      @Nullable String category, @Nullable String sourceId, @Nullable Integer sourceNumber) {
        this(id, name, searchName, category, sourceId, sourceNumber, null, false, null, null, 1, null);
    }
}
