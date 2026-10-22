package io.github.lucaslimacelino_dev.calju.data.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import io.github.lucaslimacelino_dev.calju.CaLjuApp;

/**
 * Roda o motor de sincronização em segundo plano (o WorkManager decide quando: só com rede).
 * Sem rede ou erro do servidor: "tente de novo" (espera crescente). Sem sessão: nada a fazer.
 */
public class SyncWorker extends Worker {

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        SyncEngine.Result result = ((CaLjuApp) getApplicationContext()).container().syncEngine.run();
        switch (result.outcome()) {
            case OFFLINE:
            case SERVER_ERROR:
                return Result.retry();
            default:
                return Result.success();
        }
    }
}
