package io.github.lucaslimacelino_dev.calju.data.sync;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Cursor do recebimento (último número de mudança já aplicado) e a conta à qual o banco do
 * aparelho está vinculado. Não são segredos.
 */
public class SyncPrefs {

    private static final String PREFS = "calju_sync";
    private static final String CURSOR = "cursor";
    private static final String BOUND_EMAIL = "bound_email";

    private final SharedPreferences prefs;

    public SyncPrefs(Context context) {
        this.prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public long cursor() {
        return prefs.getLong(CURSOR, 0L);
    }

    public void saveCursor(long cursor) {
        prefs.edit().putLong(CURSOR, cursor).apply();
    }

    /** Conta dona dos dados do aparelho; nula = nunca vinculado. */
    public String boundEmail() {
        return prefs.getString(BOUND_EMAIL, null);
    }

    public void bind(String email) {
        prefs.edit().putString(BOUND_EMAIL, email).apply();
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
