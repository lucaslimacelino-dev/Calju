package io.github.lucaslimacelino_dev.calju.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogImporter;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.FoodNutrientEntity;
import io.github.lucaslimacelino_dev.calju.data.local.FoodSearchRow;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

@RunWith(AndroidJUnit4.class)
public class DiaryRepositoryTest {

    private static final String DB_FILE = "diary-test.db";
    private static final LocalDate DAY = LocalDate.of(2026, 10, 20);

    private Context context;
    private CaljuDatabase database;
    private DiaryRepository diary;
    private String arrozId;

    private CaljuDatabase open() {
        return Room.databaseBuilder(context, CaljuDatabase.class, DB_FILE).build();
    }

    @Before
    public void setUp() throws Exception {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        context.deleteDatabase(DB_FILE);
        database = open();
        new CatalogImporter(database, context.getAssets()).importIfNeeded();
        diary = new DiaryRepository(database, Clock.systemUTC());
        FoodSearchRow arroz = new CatalogRepository(database.catalogDao()).search("arroz tipo 1 cozido", 1).get(0);
        arrozId = arroz.id;
    }

    @After
    public void tearDown() {
        database.close();
        context.deleteDatabase(DB_FILE);
    }

    private static BigDecimal kcal(EntryWithItems entry) {
        Intake intake = DiaryRepository.toIntake(entry.items.get(0));
        return NutritionCalculator.total(Collections.singletonList(intake), Nutrients.ENERGY).amount();
    }

    @Test
    public void registroGuardaQuantidadeOcasiaoECopiaDosNutrientes() {
        diary.addFood(DAY, Occasion.LUNCH, arrozId, new BigDecimal("150"));

        List<EntryWithItems> day = diary.day(DAY);
        assertEquals(1, day.size());
        assertEquals(Occasion.LUNCH, day.get(0).entry.occasion);
        assertEquals("Arroz, tipo 1, cozido", day.get(0).items.get(0).item.foodNameSnap);
        assertEquals(database.catalogDao().nutrientsOf(arrozId).size(), day.get(0).items.get(0).nutrients.size());
        assertEquals(0, new BigDecimal("192.387750").compareTo(kcal(day.get(0))));
    }

    @Test
    public void mudarOCatalogoNaoMudaORegistroAntigo() {
        diary.addFood(DAY, Occasion.LUNCH, arrozId, new BigDecimal("150"));
        BigDecimal before = kcal(diary.day(DAY).get(0));

        database.catalogDao().insertFoodNutrients(Collections.singletonList(
                new FoodNutrientEntity(arrozId, Nutrients.ENERGY, new BigDecimal("999"), "VALUE")));

        assertEquals(0, before.compareTo(kcal(diary.day(DAY).get(0))));
    }

    @Test
    public void editarQuantidadeEOcasiaoRecalculaEAumentaAVersao() {
        diary.addFood(DAY, Occasion.LUNCH, arrozId, new BigDecimal("150"));
        EntryWithItems entry = diary.day(DAY).get(0);

        diary.updateItem(entry.items.get(0).item.id, new BigDecimal("100"), Occasion.DINNER);

        EntryWithItems edited = diary.day(DAY).get(0);
        assertEquals(Occasion.DINNER, edited.entry.occasion);
        assertEquals(0, new BigDecimal("128.2585").compareTo(kcal(edited)));
        assertEquals(entry.entry.version + 1, edited.entry.version);
    }

    @Test
    public void removerEDesfazer() {
        String entryId = diary.addFood(DAY, Occasion.SNACK, arrozId, new BigDecimal("50"));

        diary.removeEntry(entryId);
        assertTrue(diary.day(DAY).isEmpty());

        diary.restoreEntry(entryId);
        assertEquals(1, diary.day(DAY).size());
    }

    @Test
    public void diasDiferentesNaoSeMisturam() {
        diary.addFood(DAY, Occasion.LUNCH, arrozId, new BigDecimal("150"));

        assertTrue(diary.day(DAY.plusDays(1)).isEmpty());
        assertFalse(diary.day(DAY).isEmpty());
    }

    @Test
    public void registroPersisteAposFecharEAbrirOBanco() {
        diary.addFood(DAY, Occasion.LUNCH, arrozId, new BigDecimal("150"));
        database.close();

        database = open();
        diary = new DiaryRepository(database, Clock.systemUTC());

        assertEquals(1, diary.day(DAY).size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void quantidadeZeroEhRecusada() {
        diary.addFood(DAY, Occasion.LUNCH, arrozId, BigDecimal.ZERO);
    }
}
