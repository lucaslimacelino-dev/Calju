package io.github.lucaslimacelino_dev.calju.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogImporter;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.FoodEntity;
import io.github.lucaslimacelino_dev.calju.data.local.LogEntryEntity;
import io.github.lucaslimacelino_dev.calju.data.local.NutritionGoalEntity;
import io.github.lucaslimacelino_dev.calju.data.local.SyncDao;
import io.github.lucaslimacelino_dev.calju.data.local.SyncStates;
import io.github.lucaslimacelino_dev.calju.data.remote.ApiClient;
import io.github.lucaslimacelino_dev.calju.data.remote.SessionStore;
import io.github.lucaslimacelino_dev.calju.data.remote.dto.TokenResponse;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.CustomFoodData;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.GoalRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.ItemDraft;
import io.github.lucaslimacelino_dev.calju.data.repository.MealRepository;
import io.github.lucaslimacelino_dev.calju.data.sync.ConflictResolver;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncEngine;
import io.github.lucaslimacelino_dev.calju.data.sync.SyncPrefs;
import io.github.lucaslimacelino_dev.calju.domain.Goal;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

/**
 * Conflitos, reenvio e queda no meio, com o banco de verdade e um servidor simulado: nada
 * duplica, nada é sobrescrito em silêncio e o que falhou é enviado de novo.
 */
@RunWith(AndroidJUnit4.class)
public class SyncConflictTest {

    private static final String EMPTY_PAGE =
            "{\"cursor\":0,\"hasMore\":false,\"goals\":[],\"foods\":[],\"meals\":[],\"logEntries\":[]}";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 22);

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
    private SyncDao sync;
    private MockWebServer server;
    private CatalogRepository catalog;
    private DiaryRepository diary;
    private MealRepository meals;
    private GoalRepository goals;
    private SyncPrefs prefs;
    private SyncEngine engine;
    private ConflictResolver resolver;
    private String arroz;

    private static MockResponse json(int code, String body) {
        return new MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body);
    }

    private static String meal(String id, String name, int version) {
        return "{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"items\":[{\"id\":\"" + UUID.randomUUID()
                + "\",\"foodId\":\"" + UUID.randomUUID() + "\",\"quantityG\":150}],\"version\":" + version
                + ",\"updatedAt\":\"2026-10-22T12:00:00Z\",\"deletedAt\":null}";
    }

    /** Registro como o servidor devolveria, com um item de arroz. */
    private String entry(String id, String itemId, String grams, String createdAt, int version) {
        return "{\"id\":\"" + id + "\",\"logDate\":\"2026-10-22\",\"occasion\":\"LUNCH\",\"title\":null,"
                + "\"sourceMealId\":null,\"createdAt\":\"" + createdAt + "\",\"items\":[{\"id\":\"" + itemId
                + "\",\"foodId\":\"" + arroz + "\",\"foodName\":\"Arroz, tipo 1, cozido\",\"brand\":null,"
                + "\"source\":\"TACO-4\",\"quantityG\":" + grams + ",\"portionLabel\":null,\"nutrients\":["
                + "{\"code\":\"ENERC\",\"amount\":128,\"qualifier\":\"VALUE\"}]}],\"version\":" + version
                + ",\"updatedAt\":\"2026-10-22T12:40:00Z\",\"deletedAt\":null}";
    }

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, CaljuDatabase.class).build();
        sync = database.syncDao();
        new CatalogImporter(database, context.getAssets()).importIfNeeded();
        catalog = new CatalogRepository(database, Clock.systemDefaultZone());
        arroz = catalog.search("arroz tipo 1 cozido", 1).get(0).id;
        diary = new DiaryRepository(database, Clock.systemUTC());
        meals = new MealRepository(database, Clock.systemDefaultZone());
        goals = new GoalRepository(database, Clock.systemDefaultZone());
        server = new MockWebServer();
        server.start();
        ApiClient api = new ApiClient(server.url("/").toString(), new MemoryStore());
        api.session().signIn(new TokenResponse("acesso", "Bearer", 900, "refresh", 0), "pessoa@exemplo.com");
        prefs = new SyncPrefs(context);
        prefs.clear();
        engine = new SyncEngine(database, catalog, api.data(), api.session(), prefs);
        resolver = new ConflictResolver(database, api.data());
    }

    @After
    public void tearDown() throws Exception {
        server.shutdown();
        database.close();
        prefs.clear();
    }

    private String idOfNamed(String table, String name) {
        try (Cursor cursor = database.query("SELECT id FROM " + table + " WHERE name = ?", new Object[]{name})) {
            return cursor.moveToFirst() ? cursor.getString(0) : null;
        }
    }

    // Reenvio e queda no meio

    @Test
    public void reenvioDeRegistroJaGravadoNaoDuplicaNemViraConflito() throws Exception {
        String id = diary.addFood(DAY, Occasion.LUNCH, arroz, new BigDecimal("150"));
        // 1ª rodada: o servidor grava, mas a resposta se perde (erro 500).
        server.enqueue(json(500, "{}"));
        assertEquals(SyncEngine.Outcome.SERVER_ERROR, engine.run().outcome());
        assertEquals(SyncStates.PENDING, sync.entry(id).syncState);
        JsonObject saved = JsonParser.parseString(server.takeRequest().getBody().readUtf8()).getAsJsonObject();
        saved.addProperty("id", id);
        saved.addProperty("version", 1);
        saved.addProperty("updatedAt", "2026-10-22T12:40:00Z");
        saved.add("deletedAt", JsonNull.INSTANCE);

        // 2ª rodada: o reenvio dá 409 e a cópia do servidor é igual à daqui.
        server.enqueue(json(409, "{\"status\":409,\"currentVersion\":1}"));
        server.enqueue(json(200, saved.toString()));
        server.enqueue(json(200, EMPTY_PAGE));

        SyncEngine.Result result = engine.run();

        assertEquals(SyncEngine.Outcome.DONE, result.outcome());
        assertEquals(0, result.conflicts());
        assertEquals("/api/v1/log-entries/" + id, server.takeRequest().getPath());
        RecordedRequest get = server.takeRequest();
        assertEquals("GET", get.getMethod());
        LogEntryEntity stored = sync.entry(id);
        assertEquals(SyncStates.SYNCED, stored.syncState);
        assertEquals(1, stored.serverVersion);
        assertEquals(1, diary.day(DAY).size());
    }

    @Test
    public void quedaNoMeioMantemOQueFoiEReenviaSoOQueFaltou() throws Exception {
        String a = meals.create("A", Collections.singletonList(ItemDraft.of(arroz, new BigDecimal("100"))));
        String b = meals.create("B", Collections.singletonList(ItemDraft.of(arroz, new BigDecimal("200"))));
        server.enqueue(json(201, meal(UUID.randomUUID().toString(), "x", 1)));
        server.enqueue(json(503, "{}"));

        assertEquals(SyncEngine.Outcome.SERVER_ERROR, engine.run().outcome());
        String first = server.takeRequest().getPath().endsWith(a) ? a : b;
        String second = first.equals(a) ? b : a;
        server.takeRequest();
        assertEquals(SyncStates.SYNCED, sync.meal(first).syncState);
        assertEquals(SyncStates.PENDING, sync.meal(second).syncState);

        server.enqueue(json(201, meal(second, "x", 1)));
        server.enqueue(json(200, EMPTY_PAGE));
        SyncEngine.Result result = engine.run();

        assertEquals(1, result.sent());
        assertEquals("/api/v1/meals/" + second, server.takeRequest().getPath());
        assertEquals(SyncStates.SYNCED, sync.meal(second).syncState);
        assertEquals(4, server.getRequestCount());
    }

    @Test
    public void recusadoNaoVoltaEmLacoMasPodeSerTentadoDeNovo() throws Exception {
        String id = meals.create("A", Collections.singletonList(ItemDraft.of(arroz, new BigDecimal("100"))));
        server.enqueue(json(400, "{\"status\":400,\"detail\":\"inválido\"}"));
        server.enqueue(json(200, EMPTY_PAGE));
        engine.run();
        assertEquals(SyncStates.FAILED, sync.meal(id).syncState);

        server.enqueue(json(200, EMPTY_PAGE));
        engine.run();
        assertEquals(3, server.getRequestCount());

        resolver.retryFailed();
        assertEquals(SyncStates.PENDING, sync.meal(id).syncState);
    }

    // Alimentos e refeições: a versão do servidor fica e a daqui vira cópia

    @Test
    public void alimentoEmConflitoViraCopiaComOsNutrientesDaqui() {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put(Nutrients.ENERGY, new BigDecimal("400"));
        values.put(Nutrients.PROTEIN, new BigDecimal("10"));
        values.put(Nutrients.CARBOHYDRATE, new BigDecimal("60"));
        values.put(Nutrients.FAT, new BigDecimal("12"));
        String id = catalog.createCustomFood(new CustomFoodData("Granola da casa", null, values, "1 xícara",
                new BigDecimal("40")));
        server.enqueue(json(409, "{\"status\":409}"));
        server.enqueue(json(200, "{\"id\":\"" + id + "\",\"name\":\"Granola do outro celular\",\"brand\":null,"
                + "\"nutrients\":[{\"code\":\"ENERC\",\"amount\":350,\"qualifier\":\"VALUE\"}],\"portion\":null,"
                + "\"version\":4,\"updatedAt\":\"2026-10-22T12:00:00Z\",\"deletedAt\":null}"));
        server.enqueue(json(200, EMPTY_PAGE));

        SyncEngine.Result result = engine.run();

        assertEquals(0, result.conflicts());
        FoodEntity original = sync.food(id);
        assertEquals("Granola do outro celular", original.name);
        assertEquals(SyncStates.SYNCED, original.syncState);
        assertEquals(4, original.serverVersion);
        String copy = idOfNamed("foods", "Granola da casa (conflito)");
        assertNotNull(copy);
        assertEquals(SyncStates.PENDING, sync.food(copy).syncState);
        assertEquals(4, database.catalogDao().nutrientsOf(copy).size());
        assertNotNull(database.catalogDao().defaultPortionOf(copy));
        assertEquals(1, catalog.search("granola da casa conflito", 5).size());
    }

    @Test
    public void refeicaoExcluidaAquiEEditadaLaFicaComAEdicaoSemCopia() {
        String id = meals.create("Marmita", Collections.singletonList(ItemDraft.of(arroz, new BigDecimal("150"))));
        server.enqueue(json(201, meal(id, "Marmita", 1)));
        server.enqueue(json(200, EMPTY_PAGE));
        engine.run();
        meals.delete(id);
        server.enqueue(json(409, "{\"status\":409}"));
        server.enqueue(json(200, meal(id, "Marmita editada lá", 2)));
        server.enqueue(json(200, EMPTY_PAGE));

        engine.run();

        assertEquals("Marmita editada lá", sync.meal(id).name);
        assertNull(sync.meal(id).deletedAt);
        assertNull(idOfNamed("meals", "Marmita (conflito)"));
    }

    // Registros e metas: o usuário escolhe

    @Test
    public void registroDiferenteFicaParaOUsuarioEPodeManterODaqui() throws Exception {
        String id = diary.addFood(DAY, Occasion.LUNCH, arroz, new BigDecimal("150"));
        String remote = entry(id, UUID.randomUUID().toString(), "200", "2026-10-22T12:40:00Z", 5);
        server.enqueue(json(409, "{\"status\":409}"));
        server.enqueue(json(200, remote));
        server.enqueue(json(200, EMPTY_PAGE));

        SyncEngine.Result result = engine.run();

        assertEquals(1, result.conflicts());
        assertEquals(SyncStates.CONFLICT, sync.entry(id).syncState);

        server.enqueue(json(200, remote));
        ConflictResolver.Choice choice = resolver.next();
        assertEquals(ConflictResolver.Kind.ENTRY, choice.kind());
        assertEquals(0, new BigDecimal("150").compareTo(choice.local().items().get(0).grams()));
        assertEquals(0, new BigDecimal("200").compareTo(choice.remote().items().get(0).grams()));
        assertEquals(0, new BigDecimal("256").compareTo(choice.remote().kcal()));

        resolver.keepLocal(choice);

        LogEntryEntity stored = sync.entry(id);
        assertEquals(SyncStates.PENDING, stored.syncState);
        assertEquals(5, stored.serverVersion);
        assertEquals(0, new BigDecimal("150").compareTo(diary.entry(id).items.get(0).item.quantityG));
    }

    @Test
    public void usarODaContaTrocaORegistroDaqui() throws Exception {
        String id = diary.addFood(DAY, Occasion.LUNCH, arroz, new BigDecimal("150"));
        sync.entryState(id, SyncStates.CONFLICT);
        server.enqueue(json(200, entry(id, UUID.randomUUID().toString(), "200", "2026-10-22T12:40:00Z", 5)));

        resolver.useServer(resolver.next());

        EntryWithItems stored = diary.entry(id);
        assertEquals(SyncStates.SYNCED, stored.entry.syncState);
        assertEquals(0, new BigDecimal("200").compareTo(stored.items.get(0).item.quantityG));
        assertNull(resolver.next());
    }

    @Test
    public void metaNoMesmoDiaComOutroIdManterAquiOcupaAMetaDaConta() throws Exception {
        goals.save(new Goal(DAY, new BigDecimal("2000"), null, null, null));
        String localId = database.goalDao().byEffectiveFrom(DAY).id;
        sync.goalState(localId, SyncStates.CONFLICT);
        String remoteId = UUID.randomUUID().toString();
        server.enqueue(json(404, "{\"status\":404}"));
        server.enqueue(json(200, "[{\"id\":\"" + remoteId + "\",\"effectiveFrom\":\"2026-10-22\",\"kcal\":1800,"
                + "\"proteinG\":90,\"carbohydrateG\":null,\"fatG\":null,\"version\":2,"
                + "\"updatedAt\":\"2026-10-22T12:00:00Z\"}]"));

        ConflictResolver.Choice choice = resolver.next();
        assertEquals(ConflictResolver.Kind.GOAL, choice.kind());
        assertEquals(0, new BigDecimal("1800").compareTo(choice.remote().kcal()));

        resolver.keepLocal(choice);

        assertNull(sync.goal(localId));
        NutritionGoalEntity merged = sync.goal(remoteId);
        assertEquals(0, new BigDecimal("2000").compareTo(merged.kcal));
        assertEquals(SyncStates.PENDING, merged.syncState);
        assertEquals(2, merged.serverVersion);
        assertTrue(goals.all().size() == 1);
    }
}
