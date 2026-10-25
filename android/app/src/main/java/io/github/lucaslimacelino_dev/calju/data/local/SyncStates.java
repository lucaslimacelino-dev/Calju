package io.github.lucaslimacelino_dev.calju.data.local;

/**
 * Estado de sincronização de cada registro (coluna sync_state). {@code server_version} guarda a
 * última versão confirmada pelo servidor (0 = nunca enviado).
 */
public final class SyncStates {

    /** Mudou no aparelho e ainda não foi enviado. */
    public static final String PENDING = "PENDING";
    /** Igual ao servidor. */
    public static final String SYNCED = "SYNCED";
    /** O servidor tem uma versão diferente: o usuário precisa decidir. */
    public static final String CONFLICT = "CONFLICT";
    /** O servidor recusou o registro (dado inválido); não é reenviado em laço. */
    public static final String FAILED = "FAILED";

    private SyncStates() {
    }
}
