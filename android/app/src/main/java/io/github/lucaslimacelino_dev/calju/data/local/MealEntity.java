package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/** Refeição salva (molde): nome e itens que apontam para os alimentos atuais do catálogo. */
@Entity(tableName = "meals")
public class MealEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @NonNull
    public String name;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @ColumnInfo(name = "deleted_at")
    @Nullable
    public Long deletedAt;

    public int version;

    @ColumnInfo(name = "sync_state")
    @NonNull
    public String syncState;

    /** Última versão confirmada pelo servidor; 0 = nunca enviado (ver SyncStates). */
    @ColumnInfo(name = "server_version", defaultValue = "0")
    public int serverVersion;

    public MealEntity(@NonNull String id, @NonNull String name, long createdAt, long updatedAt,
                      @Nullable Long deletedAt, int version, @NonNull String syncState) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
        this.syncState = syncState;
    }
}
