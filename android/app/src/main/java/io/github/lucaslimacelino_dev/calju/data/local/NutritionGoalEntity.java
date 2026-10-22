package io.github.lucaslimacelino_dev.calju.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Meta diária com data de início de vigência; uma por data. */
@Entity(tableName = "nutrition_goals", indices = {@Index(value = "effective_from", unique = true)})
public class NutritionGoalEntity {

    @PrimaryKey
    @NonNull
    public String id;

    @ColumnInfo(name = "effective_from")
    @NonNull
    public LocalDate effectiveFrom;

    @NonNull
    public BigDecimal kcal;

    @ColumnInfo(name = "protein_g")
    @Nullable
    public BigDecimal proteinG;

    @ColumnInfo(name = "carbs_g")
    @Nullable
    public BigDecimal carbsG;

    @ColumnInfo(name = "fat_g")
    @Nullable
    public BigDecimal fatG;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public int version;

    @ColumnInfo(name = "sync_state")
    @NonNull
    public String syncState;

    /** Última versão confirmada pelo servidor; 0 = nunca enviado (ver SyncStates). */
    @ColumnInfo(name = "server_version", defaultValue = "0")
    public int serverVersion;

    public NutritionGoalEntity(@NonNull String id, @NonNull LocalDate effectiveFrom, @NonNull BigDecimal kcal,
                               @Nullable BigDecimal proteinG, @Nullable BigDecimal carbsG,
                               @Nullable BigDecimal fatG, long updatedAt, int version, @NonNull String syncState) {
        this.id = id;
        this.effectiveFrom = effectiveFrom;
        this.kcal = kcal;
        this.proteinG = proteinG;
        this.carbsG = carbsG;
        this.fatG = fatG;
        this.updatedAt = updatedAt;
        this.version = version;
        this.syncState = syncState;
    }
}
