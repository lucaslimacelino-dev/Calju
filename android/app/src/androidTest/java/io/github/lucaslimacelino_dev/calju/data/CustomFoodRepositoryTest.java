package io.github.lucaslimacelino_dev.calju.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogImporter;
import io.github.lucaslimacelino_dev.calju.data.local.EntryWithItems;
import io.github.lucaslimacelino_dev.calju.data.local.FoodSearchRow;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.CustomFoodData;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.Intake;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.NutritionCalculator;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;
import io.github.lucaslimacelino_dev.calju.domain.PortionSource;

@RunWith(AndroidJUnit4.class)
public class CustomFoodRepositoryTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 21);

    private CaljuDatabase database;
    private CatalogRepository catalog;
    private DiaryRepository diary;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, CaljuDatabase.class).build();
        new CatalogImporter(database, context.getAssets()).importIfNeeded();
        catalog = new CatalogRepository(database, Clock.systemDefaultZone());
        diary = new DiaryRepository(database, Clock.systemDefaultZone());
    }

    @After
    public void tearDown() {
        database.close();
    }

    // Valores fictícios: só o comportamento importa.
    private static CustomFoodData granola(String kcal) {
        Map<String, BigDecimal> values = new LinkedHashMap<>();
        values.put(Nutrients.ENERGY, new BigDecimal(kcal));
        values.put(Nutrients.PROTEIN, new BigDecimal("10"));
        values.put(Nutrients.CARBOHYDRATE, new BigDecimal("60"));
        values.put(Nutrients.FAT, new BigDecimal("12"));
        return new CustomFoodData("Granola da casa", "Marca Teste", values, "1 xícara", new BigDecimal("40"));
    }

    private static BigDecimal kcalOf(EntryWithItems entry) {
        Intake intake = DiaryRepository.toIntake(entry.items.get(0));
        return NutritionCalculator.total(Collections.singletonList(intake), Nutrients.ENERGY).amount();
    }

    @Test
    public void alimentoCriadoApareceNaBuscaPeloNomeEPelaMarcaComAPorcao() {
        String id = catalog.createCustomFood(granola("400"));

        List<FoodSearchRow> byName = catalog.search("granola casa", 5);
        assertEquals(id, byName.get(0).id);
        assertEquals("Marca Teste", byName.get(0).brand);
        assertEquals(id, catalog.search("granola marca teste", 5).get(0).id);

        Food food = catalog.food(id);
        assertEquals(0, new BigDecimal("400").compareTo(food.nutrient(Nutrients.ENERGY).get().amount()));
        assertFalse("fibra não informada é desconhecida", food.nutrient(Nutrients.FIBER).isPresent());
        assertEquals("1 xícara", food.defaultPortion().label());
        assertEquals(PortionSource.USER, food.defaultPortion().source());
    }

    @Test
    public void editarMudaOAlimentoMasNaoORegistroJaFeito() {
        String id = catalog.createCustomFood(granola("400"));
        diary.addFood(DAY, Occasion.BREAKFAST, id, new BigDecimal("50"));

        catalog.updateCustomFood(id, granola("300"));

        assertEquals(0, new BigDecimal("300").compareTo(catalog.customFood(id).per100g().get(Nutrients.ENERGY)));
        EntryWithItems entry = diary.day(DAY).get(0);
        assertEquals(0, new BigDecimal("200").compareTo(kcalOf(entry)));
        assertEquals("Marca Teste", entry.items.get(0).item.brandSnap);
    }

    @Test
    public void excluirTiraDaBuscaMasOAlimentoContinuaLegivelERestaurarDevolve() {
        String id = catalog.createCustomFood(granola("400"));

        catalog.deleteCustomFood(id);
        assertTrue(catalog.search("granola casa", 5).isEmpty());
        assertNotNull("moldes antigos ainda leem o alimento", catalog.food(id));

        catalog.restoreCustomFood(id);
        assertEquals(id, catalog.search("granola casa", 5).get(0).id);
    }

    @Test
    public void alimentoDaTacoNaoEhEditavel() {
        String arroz = catalog.search("arroz tipo 1 cozido", 1).get(0).id;

        assertNull(catalog.customFood(arroz));
    }

    @Test(expected = IllegalArgumentException.class)
    public void nomeVazioEhRecusado() {
        CustomFoodData data = granola("400");
        catalog.createCustomFood(new CustomFoodData("  ", null, data.per100g(), null, null));
    }
}
