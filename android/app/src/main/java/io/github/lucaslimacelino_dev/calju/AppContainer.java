package io.github.lucaslimacelino_dev.calju;

import android.content.Context;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogImporter;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.GoalRepository;

/** Injeção de dependências manual: cria e guarda os objetos compartilhados pelo app. */
public class AppContainer {

    public final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    public final CaljuDatabase database;
    public final CatalogImporter catalogImporter;
    public final CatalogRepository catalogRepository;
    public final DiaryRepository diaryRepository;
    public final GoalRepository goalRepository;
    public final Clock clock = Clock.systemDefaultZone();

    public AppContainer(Context context) {
        database = CaljuDatabase.create(context);
        catalogImporter = new CatalogImporter(database, context.getAssets());
        catalogRepository = new CatalogRepository(database.catalogDao());
        diaryRepository = new DiaryRepository(database, clock);
        goalRepository = new GoalRepository(database, clock);
    }
}
