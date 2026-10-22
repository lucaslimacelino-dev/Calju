package io.github.lucaslimacelino_dev.calju;

import android.content.Context;
import android.os.Build;

import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.CatalogImporter;
import io.github.lucaslimacelino_dev.calju.data.remote.ApiClient;
import io.github.lucaslimacelino_dev.calju.data.remote.KeystoreSessionStore;
import io.github.lucaslimacelino_dev.calju.data.repository.AccountRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.DiaryRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.GoalRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.MealRepository;

/** Injeção de dependências manual: cria e guarda os objetos compartilhados pelo app. */
public class AppContainer {

    public final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    /** Chamadas ao servidor: separadas do banco, para uma rede lenta não atrasar as gravações locais. */
    public final ExecutorService networkExecutor = Executors.newSingleThreadExecutor();
    public final CaljuDatabase database;
    public final CatalogImporter catalogImporter;
    public final CatalogRepository catalogRepository;
    public final DiaryRepository diaryRepository;
    public final GoalRepository goalRepository;
    public final MealRepository mealRepository;
    public final ApiClient apiClient;
    public final AccountRepository accountRepository;
    public final Clock clock = Clock.systemDefaultZone();

    public AppContainer(Context context) {
        database = CaljuDatabase.create(context);
        catalogImporter = new CatalogImporter(database, context.getAssets());
        catalogRepository = new CatalogRepository(database, clock);
        diaryRepository = new DiaryRepository(database, clock);
        goalRepository = new GoalRepository(database, clock);
        mealRepository = new MealRepository(database, clock);
        apiClient = new ApiClient(BuildConfig.API_BASE_URL, new KeystoreSessionStore(context));
        accountRepository = new AccountRepository(apiClient, Build.MODEL);
    }
}
