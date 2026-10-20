package io.github.lucaslimacelino_dev.calju;

import android.app.Application;
import android.util.Log;

import java.io.IOException;

public class CaLjuApp extends Application {

    private static final String TAG = "CaLjuApp";

    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
        container.databaseExecutor.execute(() -> {
            try {
                container.catalogImporter.importIfNeeded();
            } catch (IOException | RuntimeException e) {
                Log.e(TAG, "Falha ao importar o catálogo de alimentos", e);
            }
        });
    }

    public AppContainer container() {
        return container;
    }
}
