package io.github.lucaslimacelino_dev.calju.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;

/**
 * Simula quem tem o app antigo: cria o banco de uma versão anterior com o SQL exportado pelo
 * Room (app/schemas/.../N.json) e abre com o app atual. O Room migra e valida a estrutura ao
 * abrir; qualquer diferença faz a abertura falhar.
 */
@RunWith(AndroidJUnit4.class)
public class MigrationTest {

    private static final String DB = "migration-test.db";
    private static final String SCHEMAS = "io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase/";
    private static final int CURRENT_VERSION = 3;

    private Context context;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DB);
    }

    @After
    public void tearDown() {
        context.deleteDatabase(DB);
    }

    private static JSONObject schema(int version) throws Exception {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        try (InputStream in = testContext.getAssets().open(SCHEMAS + version + ".json")) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int read; (read = in.read(buffer)) != -1; ) {
                bytes.write(buffer, 0, read);
            }
            return new JSONObject(bytes.toString(StandardCharsets.UTF_8.name())).getJSONObject("database");
        }
    }

    /** Cria o banco exatamente como o app da versão informada criaria, com um alimento. */
    private void createVersion(int version) throws Exception {
        JSONObject database = schema(version);
        try (SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(DB), null)) {
            JSONArray entities = database.getJSONArray("entities");
            for (int i = 0; i < entities.length(); i++) {
                JSONObject entity = entities.getJSONObject(i);
                String table = entity.getString("tableName");
                db.execSQL(entity.getString("createSql").replace("${TABLE_NAME}", table));
                JSONArray indices = entity.optJSONArray("indices");
                for (int j = 0; indices != null && j < indices.length(); j++) {
                    db.execSQL(indices.getJSONObject(j).getString("createSql").replace("${TABLE_NAME}", table));
                }
            }
            JSONArray setup = database.getJSONArray("setupQueries");
            for (int i = 0; i < setup.length(); i++) {
                db.execSQL(setup.getString(i));
            }
            db.execSQL("INSERT INTO foods (id, name, search_name, category, source_id, source_number) "
                    + "VALUES ('f1', 'Alimento A', 'alimento a', 'Teste', 'TACO-4', 1)");
            db.setVersion(database.getInt("version"));
        }
    }

    private void assertMigratesToCurrent(int fromVersion) throws Exception {
        createVersion(fromVersion);

        CaljuDatabase migrated = Room.databaseBuilder(context, CaljuDatabase.class, DB).build();
        try {
            assertEquals("Alimento A", migrated.catalogDao().food("f1").name);
            assertTrue(migrated.diaryDao().day(LocalDate.of(2026, 10, 20)).isEmpty());
            assertTrue(migrated.goalDao().all().isEmpty());
            assertEquals(CURRENT_VERSION, migrated.getOpenHelper().getReadableDatabase().getVersion());
        } finally {
            migrated.close();
        }
    }

    @Test
    public void daVersao1ParaAAtual() throws Exception {
        assertMigratesToCurrent(1);
    }

    @Test
    public void daVersao2ParaAAtual() throws Exception {
        assertMigratesToCurrent(2);
    }
}
