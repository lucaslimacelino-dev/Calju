package io.github.lucaslimacelino_dev.calju;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogImporter;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;

/** Injeção de dependências manual: cria e guarda os objetos compartilhados pelo app. */
public class AppContainer {

    public final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    public final CaljuDatabase database;
    public final CatalogImporter catalogImporter;
    public final CatalogRepository catalogRepository;

    public AppContainer(Context context) {
        database = CaljuDatabase.create(context);
        catalogImporter = new CatalogImporter(database, context.getAssets());
        catalogRepository = new CatalogRepository(database.catalogDao());
    }
}
