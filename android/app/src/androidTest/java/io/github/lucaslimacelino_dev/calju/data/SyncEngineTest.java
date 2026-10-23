package io.github.lucaslimacelino_dev.calju.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogImporter;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.MealEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.SyncStates;
import io.github.lucaslimacelino_dev.calju.data.remote.ApiClient;
import io.github.lucaslimacelino_dev.calju.data.remote.SessionStore;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.TokenResponse;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.ItemDraft;
import io.github.lucaslimacelino_dev.calju.data.repository.MealRepository;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncEngine;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncPrefs;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/** Motor de sincronização com o banco de verdade e um servidor simulado (MockWebServer). */
@RunWith(AndroidJUnit4.class)
public class SyncEngineTest {

    private static final String EMPTY_PAGE = page(0, false, "[]", "[]");

    /** Sessão na memória: o motor só precisa saber que há conta conectada. */
    private static final class MemoryStore implements SessionStore {
        String token = "refresh";

        @Override
        public String refreshToken() {
            return token;
        }

        @Override
        public String email() {
            return "pessoa@exemplo.com";
        }

        @Override
        public void save(String refreshToken, String email) {
            token = refreshToken;
        }

        @Override
        public void clear() {
            token = null;
        }
    }

    private CaljuDatabase database;
    private MockWebServer server;
    private MealRepository meals;
    private SyncPrefs prefs;
    private SyncEngine engine;
    private String arroz;

    private static String page(long cursor, boolean hasMore, String meals, String entries) {
        return "{\"cursor\":" + cursor + ",\"hasMore\":" + hasMore + ",\"goals\":[],\"foods\":[],\"meals\":" + meals
                + ",\"logEntries\":" + entries + "}";
    }

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }

    private static String meal(String id, String name, int version, String deletedAt, String foodId) {
        return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"items\":[{\"id\":\"" + UUID.randomUUID()
                + "\",\"foodId\":\"" + foodId + "\",\"quantityG\":150}],\"version\":" + version
                + ",\"updatedAt\":\"2026-10-22T12:00:00Z\",\"deletedAt\":"
                + (deletedAt == null ? "null" : "\"" + deletedAt + "\"") + "}";
    }

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, CaljuDatabase.class).build();
        new CatalogImporter(database, context.getAssets()).importIfNeeded();
        CatalogRepository catalog = new CatalogRepository(database, Clock.systemDefaultZone());
        arroz = catalog.search("arroz tipo 1 cozido", 1).get(0).id;
        meals = new MealRepository(database, Clock.systemDefaultZone());
        server = new MockWebServer();
        server.start();
        ApiClient api = new ApiClient(server.url("/").toString(), new MemoryStore());
        api.session().signIn(new TokenResponse("acesso", "Bearer", 900, "refresh", 0), "pessoa@exemplo.com");
        prefs = new SyncPrefs(context);
        prefs.clear();
        engine = new SyncEngine(database, catalog, api.data(), api.session(), prefs);
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        database.close();
        prefs.clear();
    }

    private String newMeal() {
        return meals.create("Marmita", Collections.singletonList(ItemDraft.of(arroz, new BigDecimal("150"))));
    }

    @Test
    public void refeicaoNovaVaiComVersao0EFicaSincronizada() throws Exception {
        String id = newMeal();
        server.enqueue(json(201, meal(id, "Marmita", 1, null, arroz)));
        server.enqueue(json(200, EMPTY_PAGE));

        SyncEngine.Result result = engine.run();

        assertEquals(SyncEngine.Outcome.DONE, result.outcome());
        assertEquals(1, result.sent());
        RecordedRequest put = server.takeRequest();
        assertEquals("PUT", put.getMethod());
        assertEquals("/api/v1/meals/" + id, put.getPath());
        assertEquals("Bearer acesso", put.getHeader("Authorization"));
        assertTrue(put.getBody().readUtf8().contains("\"version\":0"));
        MealEntity stored = database.syncDao().meal(id);
        assertEquals(SyncStates.SYNCED, stored.syncState);
        assertEquals(1, stored.serverVersion);
    }

    @Test
    public void criadaEExcluidaAntesDeEnviarNaoVaiAoServidor() throws Exception {
        String id = newMeal();
        meals.delete(id);
        server.enqueue(json(200, EMPTY_PAGE));

        engine.run();

        assertEquals(1, server.getRequestCount());
        assertTrue(server.takeRequest().getPath().startsWith("/api/v1/sync/pull"));
        assertEquals(SyncStates.SYNCED, database.syncDao().meal(id).syncState);
    }

    @Test
    public void refeicaoEmConflitoFicaComADoServidorEADaquiViraCopia() throws Exception {
        String id = newMeal();
        server.enqueue(json(409, "{\"status\":409,\"detail\":\"mudou\",\"currentVersion\":3}"));
        server.enqueue(json(200, meal(id, "Marmita do outro celular", 3, null, arroz)));
        server.enqueue(json(200, EMPTY_PAGE));

        SyncEngine.Result result = engine.run();

        assertEquals(0, result.conflicts());
        server.takeRequest();
        assertEquals("GET", server.takeRequest().getMethod());
        MealEntity stored = database.syncDao().meal(id);
        assertEquals("Marmita do outro celular", stored.name);
        assertEquals(SyncStates.SYNCED, stored.syncState);
        assertEquals(3, stored.serverVersion);
        MealWithItems copy = meals.all().stream().filter(m -> m.meal.name.equals("Marmita (conflito)"))
                .findFirst().get();
        assertEquals(SyncStates.PENDING, copy.meal.syncState);
        assertEquals(1, copy.items.size());
    }

    @Test
    public void semRedeNadaSePerde() throws Exception {
        String id = newMeal();
        server.shutdown();

        SyncEngine.Result result = engine.run();

        assertEquals(SyncEngine.Outcome.OFFLINE, result.outcome());
        assertEquals(SyncStates.PENDING, database.syncDao().meal(id).syncState);
    }

    @Test
    public void recebimentoAplicaNovosELapidesMasNaoPisaEmMudancaLocal() throws Exception {
        String pendingLocal = newMeal();
        String synced = UUID.randomUUID().toString();
        String remoteNew = UUID.randomUUID().toString();
        String entry = UUID.randomUUID().toString();
        // 1ª rodada: a refeição local vai e a "synced" chega do servidor.
        server.enqueue(json(201, meal(pendingLocal, "Marmita", 1, null, arroz)));
        server.enqueue(json(200, page(10, false, "[" + meal(synced, "Café", 1, null, arroz) + "]", "[]")));
        engine.run();
        meals.update(pendingLocal, "Marmita editada aqui",
                Collections.singletonList(ItemDraft.of(arroz, new BigDecimal("100"))));

        String entryJson = "{\"id\":\"" + entry + "\",\"logDate\":\"2026-10-22\",\"occasion\":\"LUNCH\",\"title\":null,"
                + "\"sourceMealId\":null,\"createdAt\":\"2026-10-22T12:40:00Z\",\"items\":[{\"id\":\""
                + UUID.randomUUID() + "\",\"foodId\":\"" + arroz + "\",\"foodName\":\"Arroz, tipo 1, cozido\","
                + "\"brand\":null,\"source\":\"TACO-4\",\"quantityG\":150,\"portionLabel\":null,\"nutrients\":["
                + "{\"code\":\"ENERC\",\"amount\":128,\"qualifier\":\"VALUE\"}]}],\"version\":1,"
                + "\"updatedAt\":\"2026-10-22T12:40:00Z\",\"deletedAt\":null}";
        // 2ª rodada: o servidor também mudou a refeição local (a edição daqui vira cópia),
        // apagou a "synced" e trouxe uma refeição e um registro novos.
        server.enqueue(json(409, "{\"status\":409,\"detail\":\"mudou\"}"));
        server.enqueue(json(200, meal(pendingLocal, "Marmita do servidor", 2, null, arroz)));
        server.enqueue(json(200, page(20, false, "[" + meal(pendingLocal, "Marmita do servidor", 2, null, arroz) + ","
                + meal(synced, "Café", 2, "2026-10-22T13:00:00Z", arroz) + ","
                + meal(remoteNew, "Lanche", 1, null, arroz) + "]", "[" + entryJson + "]")));

        engine.run();

        assertEquals("Marmita do servidor", database.syncDao().meal(pendingLocal).name);
        assertTrue(meals.all().stream().anyMatch(m -> m.meal.name.equals("Marmita editada aqui (conflito)")));
        assertNotNull(database.syncDao().meal(synced).deletedAt);
        assertEquals("Lanche", database.syncDao().meal(remoteNew).name);
        EntryWithItems received = database.diaryDao().day(LocalDate.of(2026, 10, 22)).get(0);
        assertEquals("Arroz, tipo 1, cozido", received.items.get(0).item.foodNameSnap);
        assertEquals(1, received.items.get(0).nutrients.size());
        assertEquals(20, prefs.cursor());
    }

    @Test
    public void paginasSeguemOCursor() throws Exception {
        server.enqueue(json(200, page(5, true, "[" + meal(UUID.randomUUID().toString(), "A", 1, null, arroz) + "]", "[]")));
        server.enqueue(json(200, page(9, false, "[" + meal(UUID.randomUUID().toString(), "B", 1, null, arroz) + "]", "[]")));

        SyncEngine.Result result = engine.run();

        assertEquals(2, result.received());
        assertTrue(server.takeRequest().getPath().contains("cursor=0"));
        assertTrue(server.takeRequest().getPath().contains("cursor=5"));
        assertEquals(9, prefs.cursor());
    }
}
