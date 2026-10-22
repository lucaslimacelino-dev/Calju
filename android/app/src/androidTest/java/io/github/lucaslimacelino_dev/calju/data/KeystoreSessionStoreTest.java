package io.github.lucaslimacelino_dev.calju.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import android.content.Context;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.data.remote.KeystoreSessionStore;

/** O refresh token volta igual depois de cifrado, e o arquivo nunca o contém em texto puro. */
@RunWith(AndroidJUnit4.class)
public class KeystoreSessionStoreTest {

    private static final String TOKEN = "refresh-token-de-teste-1234567890";

    private Context context;
    private KeystoreSessionStore store;

    @Before
    public void setUp() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        store = new KeystoreSessionStore(context);
        store.clear();
    }

    @After
    public void tearDown() {
        store.clear();
    }

    @Test
    public void guardaELeOTokenCifrado() {
        store.save(TOKEN, "pessoa@exemplo.com");

        assertEquals(TOKEN, new KeystoreSessionStore(context).refreshToken());
        assertEquals("pessoa@exemplo.com", store.email());
        Map<String, ?> raw = context.getSharedPreferences("calju_auth", Context.MODE_PRIVATE).getAll();
        for (Object value : raw.values()) {
            assertFalse(String.valueOf(value).contains(TOKEN));
        }
    }

    @Test
    public void limparApagaASessao() {
        store.save(TOKEN, "pessoa@exemplo.com");
        store.clear();

        assertNull(store.refreshToken());
        assertNull(store.email());
    }
}
