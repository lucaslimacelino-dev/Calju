package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.time.LocalDate;

import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/** Um lançamento do diário (data + ocasião), com um ou mais itens. */
@Entity(tableName = "log_entries", indices = {@Index("log_date")})
public class LogEntryEntity {

    public static final String SYNC_PENDING = "PENDING";

    @PrimaryKey
    @NonNull
    public String id;

    /** Dia do usuário, sem fuso horário. */
    @ColumnInfo(name = "log_date")
    @NonNull
    public LocalDate logDate;

    @NonNull
    public Occasion occasion;

    @Nullable
    public String title;

    @ColumnInfo(name = "source_meal_id")
    @Nullable
    public String sourceMealId;

    /** Instantes em milissegundos desde 1970 (UTC). */
    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    /** Preenchido na exclusão (lápide para a sincronização); nulo = ativo. */
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

    public LogEntryEntity(@NonNull String id, @NonNull LocalDate logDate, @NonNull Occasion occasion,
                          @Nullable String title, @Nullable String sourceMealId, long createdAt, long updatedAt,
                          @Nullable Long deletedAt, int version, @NonNull String syncState) {
        this.id = id;
        this.logDate = logDate;
        this.occasion = occasion;
        this.title = title;
        this.sourceMealId = sourceMealId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
        this.version = version;
        this.syncState = syncState;
    }
}
