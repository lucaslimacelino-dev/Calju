package io.github.lucaslimacelino_dev.calju.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import androidx.room.Room;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import java.math.BigDecimal;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogImporter;
import io.github.lucaslimacelino_dev.calju.data.local.FoodSearchRow;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.domain.Food;
import io.github.lucaslimacelino_dev.calju.domain.FoodInputParser;
import io.github.lucaslimacelino_dev.calju.domain.Nutrients;
import io.github.lucaslimacelino_dev.calju.domain.Qualifier;

/** Usa os CSVs reais embutidos no APK, num banco em memória. */
@RunWith(AndroidJUnit4.class)
public class CatalogDatabaseTest {

    private CaljuDatabase database;
    private CatalogImporter importer;
    private CatalogRepository repository;

    @Before
    public void setUp() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        database = Room.inMemoryDatabaseBuilder(context, CaljuDatabase.class).build();
        importer = new CatalogImporter(database, context.getAssets());
        repository = new CatalogRepository(database.catalogDao());
        assertTrue(importer.importIfNeeded());
    }

    @After
    public void tearDown() {
        database.close();
    }

    private FoodSearchRow first(String query) {
        List<FoodSearchRow> results = repository.search(query, 10);
        assertFalse("sem resultados para " + query, results.isEmpty());
        return results.get(0);
    }

    @Test
    public void importaOCatalogoCompleto() {
        assertEquals(596, database.catalogDao().countFoods());
        assertEquals(24, database.catalogDao().countNutrients());
        assertEquals(50, database.catalogDao().countPortions());
    }

    @Test
    public void segundaImportacaoNaoDuplica() throws Exception {
        assertFalse(importer.importIfNeeded());
        assertEquals(596, database.catalogDao().countFoods());
    }

    @Test
    public void buscaPorPalavrasEmQualquerOrdem() {
        assertEquals("Arroz, tipo 1, cozido", first("arroz tipo 1 cozido").name);
        assertEquals("Arroz, tipo 1, cozido", first("cozido arroz tipo").name);
    }

    @Test
    public void buscaSemAcento() {
        assertTrue(first("maca fuji").name.startsWith("Maçã, Fuji"));
    }

    @Test
    public void sugestaoMostraPorcaoPadrao() {
        FoodSearchRow banana = first("banana prata");
        assertEquals("Banana, prata, crua", banana.name);
        assertEquals("1 unidade", banana.portionLabel);
        assertEquals(0, new BigDecimal("75").compareTo(banana.portionGrams));
    }

    @Test
    public void valoresDoAlimentoChegamAoDominio() {
        Food arroz = repository.food(first("arroz tipo 1 cozido").id);

        assertEquals(0, new BigDecimal("128.2585").compareTo(arroz.nutrient(Nutrients.ENERGY).get().amount()));
        assertEquals(Qualifier.TRACE, arroz.nutrient("THIA").get().qualifier());
        assertFalse("NA na TACO vira desconhecido", arroz.nutrient("CHOLE").isPresent());
    }

    @Test
    public void textoDigitadoComUnidadesUsaAPorcaoDoIbge() {
        Food banana = repository.food(first("banana prata").id);

        BigDecimal grams = FoodInputParser.parse("2 bananas prata").gramsFor(banana).get();

        assertEquals(0, new BigDecimal("150").compareTo(grams));
    }

    @Test
    public void alimentoSemPorcaoPedeQuantidade() {
        Food arroz = repository.food(first("arroz tipo 1 cozido").id);

        assertNotNull(arroz);
        assertFalse(FoodInputParser.parse("arroz").gramsFor(arroz).isPresent());
    }
}
