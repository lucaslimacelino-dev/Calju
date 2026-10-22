package io.github.lucaslimacelino_dev.calju.data.sync;

import android.content.Context;
import android.content.SharedPreferences;

/** Cursor do recebimento (último número de mudança já aplicado). Não é segredo. */
public class SyncPrefs {

    private static final String PREFS = "calju_sync";
    private static final String CURSOR = "cursor";

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

    public void clear() {
        prefs.edit().clear().apply();
    }
}
