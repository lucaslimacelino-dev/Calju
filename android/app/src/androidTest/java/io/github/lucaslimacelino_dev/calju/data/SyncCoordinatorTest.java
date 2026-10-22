package io.github.lucaslimacelino_dev.calju.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collections;
import java.util.concurrent.Executor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogImporter;
import io.github.lucaslimacelino_dev.calju.data.remote.ApiClient;
import io.github.lucaslimacelino_dev.calju.data.remote.SessionStore;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.TokenResponse;
import io.github.lucaslimacelino_dev.calju.data.repository.AccountRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.ItemDraft;
import io.github.lucaslimacelino_dev.calju.data.repository.MealRepository;
import io.github.lucaslimacelino_dev.calju.data.sync.AccountBinding;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncCoordinator;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncEngine;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncPrefs;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncScheduler;

/** Vínculo entre o banco do aparelho e a conta: o que perguntar, apagar e manter. */
@RunWith(AndroidJUnit4.class)
public class SyncCoordinatorTest {

    /** Porta fechada: qualquer chamada falha na hora, como sem internet. */
    private static final String OFFLINE_URL = "http://127.0.0.1:9/";

    private static final class MemoryStore implements SessionStore {
        String token;
        String email;

        @Override
        public String refreshToken() {
            return token;
        }

        @Override
        public String email() {
            return email;
        }

        @Override
        public void save(String refreshToken, String email) {
            this.token = refreshToken;
            this.email = email;
        }

        @Override
        public void clear() {
            token = null;
            email = null;
        }
    }

    private CaljuDatabase database;
    private SyncPrefs prefs;
    private ApiClient api;
    private MealRepository meals;
    private CatalogRepository catalog;
    private SyncCoordinator coordinator;
    private String arroz;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, CaljuDatabase.class).build();
        new CatalogImporter(database, context.getAssets()).importIfNeeded();
        catalog = new CatalogRepository(database, Clock.systemDefaultZone());
        arroz = catalog.search("arroz tipo 1 cozido", 1).get(0).id;
        meals = new MealRepository(database, Clock.systemDefaultZone());
        prefs = new SyncPrefs(context);
        prefs.clear();
        api = new ApiClient(OFFLINE_URL, new MemoryStore());
        api.session().signIn(new TokenResponse("acesso", "Bearer", 900, "refresh", 0), "a@exemplo.com");
        AccountRepository account = new AccountRepository(api, "Teste");
        SyncEngine engine = new SyncEngine(database, catalog, api.data(), api.session(), prefs);
        Executor direct = Runnable::run;
        coordinator = new SyncCoordinator(database, catalog, account, engine, new SyncScheduler(context), prefs,
                direct);
    }

    @After
    public void tearDown() {
        database.close();
        prefs.clear();
    }

    private void addMeal() {
        meals.create("Marmita", Collections.singletonList(ItemDraft.of(arroz, new BigDecimal("150"))));
    }

    @Test
    public void primeiraContaComDadosPerguntaESoVinculaDepoisDaEscolha() {
        addMeal();

        assertEquals(AccountBinding.Decision.ASK_UPLOAD, coordinator.afterSignIn("a@exemplo.com"));
        assertNull(prefs.boundEmail());

        coordinator.uploadLocalData("a@exemplo.com");
        assertEquals("a@exemplo.com", prefs.boundEmail());
        assertEquals(1, meals.all().size());
    }

    @Test
    public void apagarDesteCelularTiraOsDadosMasNuncaOCatalogo() {
        addMeal();
        int foodsBefore = catalog.search("arroz", 50).size();

        coordinator.replaceLocalData("a@exemplo.com");

        assertTrue(meals.all().isEmpty());
        assertEquals(0, database.syncDao().userDataCount());
        assertEquals(foodsBefore, catalog.search("arroz", 50).size());
        assertEquals("a@exemplo.com", prefs.boundEmail());
    }

    @Test
    public void semDadosVinculaDireto() {
        assertEquals(AccountBinding.Decision.CONTINUE, coordinator.afterSignIn("a@exemplo.com"));
        assertEquals("a@exemplo.com", prefs.boundEmail());
    }

    @Test
    public void outraContaComDadosAvisaECancelarEncerraASessao() {
        prefs.bind("antiga@exemplo.com");
        addMeal();

        assertEquals(AccountBinding.Decision.OTHER_ACCOUNT, coordinator.afterSignIn("a@exemplo.com"));
        coordinator.cancelSignIn();

        assertFalse(api.session().isSignedIn());
        assertEquals("antiga@exemplo.com", prefs.boundEmail());
        assertEquals(1, meals.all().size());
    }

    @Test
    public void sairMantendoGuardaDadosEVinculo() {
        coordinator.afterSignIn("a@exemplo.com");
        addMeal();

        coordinator.signOut(true);

        assertFalse(api.session().isSignedIn());
        assertEquals(1, meals.all().size());
        assertEquals("a@exemplo.com", prefs.boundEmail());
    }

    @Test
    public void sairApagandoLimpaDadosEVinculo() {
        coordinator.afterSignIn("a@exemplo.com");
        addMeal();

        coordinator.signOut(false);

        assertFalse(api.session().isSignedIn());
        assertTrue(meals.all().isEmpty());
        assertNull(prefs.boundEmail());
    }
}
