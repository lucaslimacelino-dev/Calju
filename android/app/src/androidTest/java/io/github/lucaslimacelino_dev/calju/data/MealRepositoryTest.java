package io.github.lucaslimacelino_dev.calju.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogImporter;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.ItemWithNutrients;
import io.github.lucaslimacelino_dev.calju.data.local.MealItemEntity;
import io.github.lucaslimacelino_dev.calju.data.local.MealWithItems;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.ItemDraft;
import io.github.lucaslimacelino_dev.calju.data.repository.MealRepository;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

@RunWith(AndroidJUnit4.class)
public class MealRepositoryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);

    private CaljuDatabase database;
    private MealRepository meals;
    private DiaryRepository diary;
    private String arroz;
    private String feijao;
    private String banana;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, CaljuDatabase.class).build();
        new CatalogImporter(database, context.getAssets()).importIfNeeded();
        meals = new MealRepository(database, Clock.systemUTC());
        diary = new DiaryRepository(database, Clock.systemUTC());
        CatalogRepository catalog = new CatalogRepository(database, Clock.systemDefaultZone());
        arroz = catalog.search("arroz tipo 1 cozido", 1).get(0).id;
        feijao = catalog.search("feijao carioca cozido", 1).get(0).id;
        banana = catalog.search("banana prata", 1).get(0).id;
    }

    @After
    public void tearDown() {
        database.close();
    }

    private static List<ItemDraft> marmita(String arroz, String feijao) {
        return Arrays.asList(ItemDraft.of(arroz, new BigDecimal("150")), ItemDraft.of(feijao, new BigDecimal("100")));
    }

    private static BigDecimal gramsOf(MealWithItems meal, String foodId) {
        for (MealItemEntity item : meal.items) {
            if (item.foodId.equals(foodId)) {
                return item.quantityG;
            }
        }
        return null;
    }

    @Test
    public void criaListaEOrdenaPeloNome() {
        meals.create("Marmita", marmita(arroz, feijao));
        meals.create("Café de sempre", Collections.singletonList(ItemDraft.of(banana, new BigDecimal("75"))));

        List<MealWithItems> all = meals.all();
        assertEquals(Arrays.asList("Café de sempre", "Marmita"), Arrays.asList(all.get(0).meal.name, all.get(1).meal.name));
        assertEquals(2, all.get(1).items.size());
    }

    @Test
    public void nomeEItensSaoObrigatorios() {
        assertThrows(IllegalArgumentException.class, () -> meals.create("  ", marmita(arroz, feijao)));
        assertThrows(IllegalArgumentException.class, () -> meals.create("Vazia", Collections.emptyList()));
    }

    @Test
    public void editarOMoldeSubstituiItensEAumentaAVersao() {
        String id = meals.create("Marmita", marmita(arroz, feijao));

        meals.update(id, "Marmita grande", Collections.singletonList(ItemDraft.of(arroz, new BigDecimal("200"))));

        MealWithItems meal = meals.byId(id);
        assertEquals("Marmita grande", meal.meal.name);
        assertEquals(1, meal.items.size());
        assertEquals(0, new BigDecimal("200").compareTo(gramsOf(meal, arroz)));
        assertEquals(2, meal.meal.version);
    }

    @Test
    public void duplicarGeraNovosIdentificadores() {
        String id = meals.create("Marmita", marmita(arroz, feijao));

        String copyId = meals.duplicate(id, "Marmita (cópia)");

        MealWithItems original = meals.byId(id);
        MealWithItems copy = meals.byId(copyId);
        assertNotEquals(id, copyId);
        Set<String> originalItemIds = new HashSet<>();
        for (MealItemEntity item : original.items) {
            originalItemIds.add(item.id);
        }
        for (MealItemEntity item : copy.items) {
            assertFalse(originalItemIds.contains(item.id));
        }
        assertEquals(original.items.size(), copy.items.size());
    }

    @Test
    public void excluirEDesfazer() {
        String id = meals.create("Marmita", marmita(arroz, feijao));

        meals.delete(id);
        assertTrue(meals.all().isEmpty());

        meals.restore(id);
        assertEquals(1, meals.all().size());
    }

    @Test
    public void adicionarAoDiaComAjusteNaoAlteraOMolde() {
        String id = meals.create("Marmita", marmita(arroz, feijao));

        diary.addItems(DAY, Occasion.LUNCH, "Marmita", id, Arrays.asList(
                ItemDraft.of(arroz, new BigDecimal("200")), ItemDraft.of(feijao, new BigDecimal("100"))));

        EntryWithItems entry = diary.day(DAY).get(0);
        assertEquals("Marmita", entry.entry.title);
        assertEquals(id, entry.entry.sourceMealId);
        assertEquals(2, entry.items.size());
        assertEquals(0, new BigDecimal("150").compareTo(gramsOf(meals.byId(id), arroz)));
    }

    @Test
    public void alterarOMoldeNaoAlteraORegistroJaFeito() {
        String id = meals.create("Marmita", marmita(arroz, feijao));
        diary.addItems(DAY, Occasion.LUNCH, "Marmita", id, marmita(arroz, feijao));

        meals.update(id, "Marmita", Collections.singletonList(ItemDraft.of(banana, new BigDecimal("75"))));

        EntryWithItems entry = diary.day(DAY).get(0);
        assertEquals(2, entry.items.size());
        assertEquals("Arroz, tipo 1, cozido", entry.items.get(0).item.foodNameSnap);
    }

    @Test
    public void editarRegistroMantemCopiaRemoveEIncluiItens() {
        String entryId = diary.addItems(DAY, Occasion.LUNCH, "Marmita", null, marmita(arroz, feijao));
        EntryWithItems entry = diary.day(DAY).get(0);
        ItemWithNutrients arrozItem = entry.items.get(0).item.foodId.equals(arroz) ? entry.items.get(0) : entry.items.get(1);
        int nutrientsBefore = arrozItem.nutrients.size();

        diary.updateEntry(entryId, Occasion.DINNER, Arrays.asList(
                new ItemDraft(arrozItem.item.id, arroz, new BigDecimal("120")),
                ItemDraft.of(banana, new BigDecimal("75"))));

        EntryWithItems edited = diary.day(DAY).get(0);
        assertEquals(Occasion.DINNER, edited.entry.occasion);
        assertEquals(2, edited.items.size());
        boolean hasFeijao = false;
        for (ItemWithNutrients item : edited.items) {
            hasFeijao |= item.item.foodId.equals(feijao);
            if (item.item.id.equals(arrozItem.item.id)) {
                assertEquals(0, new BigDecimal("120").compareTo(item.item.quantityG));
                assertEquals(nutrientsBefore, item.nutrients.size());
            }
        }
        assertFalse("o feijão foi removido", hasFeijao);
    }

    @Test
    public void editarRegistroSemItensRemoveORegistro() {
        String entryId = diary.addItems(DAY, Occasion.LUNCH, "Marmita", null, marmita(arroz, feijao));

        diary.updateEntry(entryId, Occasion.LUNCH, Collections.emptyList());

        assertTrue(diary.day(DAY).isEmpty());
    }
}
