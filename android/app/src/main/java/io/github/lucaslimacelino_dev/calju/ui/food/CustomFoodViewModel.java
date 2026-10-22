package io.github.lucaslimacelino_dev.calju.ui.food;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.concurrent.Executor;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.CustomFoodData;

/** Carrega, grava e exclui um alimento do usuário fora da thread da tela. */
public class CustomFoodViewModel extends ViewModel {

    /** Resultado de uma ação concluída; {@code created} distingue criação de edição. */
    public record Done(boolean deleted, boolean created, String foodId, String name) {
    }

    private final CatalogRepository catalog;
    private final Executor executor;
    private final MutableLiveData<CustomFoodData> loaded = new MutableLiveData<>();
    private final MutableLiveData<Done> done = new MutableLiveData<>();
    private boolean loadStarted;

    public CustomFoodViewModel(CatalogRepository catalog, Executor executor) {
        this.catalog = catalog;
        this.executor = executor;
    }

    public LiveData<CustomFoodData> loaded() {
        return loaded;
    }

    public LiveData<Done> done() {
        return done;
    }

    /** Lê o alimento uma única vez, mesmo se a tela for recriada (ex.: girar o aparelho). */
    public void load(String foodId) {
        if (loadStarted) {
            return;
        }
        loadStarted = true;
        executor.execute(() -> loaded.postValue(catalog.customFood(foodId)));
    }

    public void save(String foodId, CustomFoodData data) {
        executor.execute(() -> {
            String id = foodId;
            if (id == null) {
                id = catalog.createCustomFood(data);
            } else {
                catalog.updateCustomFood(id, data);
            }
            done.postValue(new Done(false, foodId == null, id, data.name().trim()));
        });
    }

    public void delete(String foodId, String name) {
        executor.execute(() -> {
            catalog.deleteCustomFood(foodId);
            done.postValue(new Done(true, false, foodId, name));
        });
    }

    public void doneHandled() {
        done.setValue(null);
    }

    public static ViewModelProvider.Factory factory(AppContainer container) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new CustomFoodViewModel(container.catalogRepository, container.databaseExecutor);
            }
        };
    }
}
