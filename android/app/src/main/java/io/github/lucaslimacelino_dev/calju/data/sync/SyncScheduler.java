package io.github.lucaslimacelino_dev.calju.data.sync;

import android.content.Context;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

/**
 * Agenda a sincronização no WorkManager: uma rodada logo depois de mudanças locais (vários
 * toques seguidos viram uma rodada só) e uma periódica, a cada 6 horas. Sempre só com rede.
 */
public class SyncScheduler {

    private static final String SOON = "calju-sync-soon";
    private static final String PERIODIC = "calju-sync-periodic";
    private static final long SOON_DELAY_SECONDS = 5;

    private final Context context;

    public SyncScheduler(Context context) {
        this.context = context.getApplicationContext();
    }

    private static Constraints online() {
        return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build();
    }

    /** Mudou algo aqui: sincroniza em alguns segundos (um novo pedido substitui o que ainda espera). */
    public void requestSoon() {
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(SyncWorker.class)
                .setConstraints(online())
                .setInitialDelay(SOON_DELAY_SECONDS, TimeUnit.SECONDS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build();
        WorkManager.getInstance(context).enqueueUniqueWork(SOON, ExistingWorkPolicy.REPLACE, request);
    }

    /** Rodada periódica para receber o que mudou em outros aparelhos. */
    public void schedulePeriodic() {
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(SyncWorker.class, 6, TimeUnit.HOURS)
                .setConstraints(online())
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(PERIODIC, ExistingPeriodicWorkPolicy.KEEP, request);
    }

    /** Saiu da conta: nada mais roda em segundo plano. */
    public void cancelAll() {
        WorkManager.getInstance(context).cancelUniqueWork(SOON);
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC);
    }
}
