package io.github.lucaslimacelino_dev.calju.data.sync;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;
import androidx.room.InvalidationTracker;

import java.util.Set;
import java.util.concurrent.Executor;

import io.github.lucaslimacelino_dev.calju.data.local.CaljuDatabase;
import io.github.lucaslimacelino_dev.calju.data.local.SyncDao;
import io.github.lucaslimacelino_dev.calju.data.repository.AccountRepository;
import io.github.lucaslimacelino_dev.calju.data.repository.CatalogRepository;

/**
 * Liga a conta ao banco do aparelho: o que fazer com os dados ao entrar e ao sair, e quando
 * sincronizar sozinho. O banco local pertence a uma conta por vez (ver {@link AccountBinding}).
 */
public class SyncCoordinator {

    private final CaljuDatabase database;
    private final SyncDao sync;
    private final CatalogRepository catalog;
    private final AccountRepository account;
    private final SyncEngine engine;
    private final SyncScheduler scheduler;
    private final SyncPrefs prefs;
    private final Executor databaseExecutor;

    public SyncCoordinator(CaljuDatabase database, CatalogRepository catalog, AccountRepository account,
                           SyncEngine engine, SyncScheduler scheduler, SyncPrefs prefs, Executor databaseExecutor) {
        this.database = database;
        this.sync = database.syncDao();
        this.catalog = catalog;
        this.account = account;
        this.engine = engine;
        this.scheduler = scheduler;
        this.prefs = prefs;
        this.databaseExecutor = databaseExecutor;
    }

    /** Ao abrir o app: com conta e banco vinculado, a sincronização periódica continua agendada. */
    public void onAppStart() {
        databaseExecutor.execute(() -> {
            if (account.session().isSignedIn() && prefs.boundEmail() != null) {
                scheduler.schedulePeriodic();
            }
        });
    }

    /**
     * Toda mudança nas tabelas sincronizadas: se há algo pendente e conta conectada, agenda uma
     * rodada. O que chega do servidor entra como sincronizado e não dispara outra rodada à toa.
     */
    public void watchLocalChanges() {
        database.getInvalidationTracker().addObserver(new InvalidationTracker.Observer(
                "log_entries", "log_items", "meals", "meal_items", "nutrition_goals", "foods") {
            @Override
            public void onInvalidated(@NonNull Set<String> tables) {
                databaseExecutor.execute(() -> {
                    if (prefs.boundEmail() != null && account.session().isSignedIn() && sync.pendingCount() > 0) {
                        scheduler.requestSoon();
                    }
                });
            }
        });
    }

    /** Logo depois de entrar. CONTINUE já vincula e começa a sincronizar; os outros pedem escolha. */
    @WorkerThread
    public AccountBinding.Decision afterSignIn(String email) {
        AccountBinding.Decision decision = AccountBinding.decide(prefs.boundEmail(), email, sync.userDataCount() > 0);
        if (decision == AccountBinding.Decision.CONTINUE) {
            if (prefs.boundEmail() == null || !prefs.boundEmail().equalsIgnoreCase(email)) {
                prefs.clear();
            }
            bindAndStart(email);
        }
        return decision;
    }

    /** "Enviar para a conta": os dados do aparelho (todos pendentes) sobem na próxima rodada. */
    @WorkerThread
    public void uploadLocalData(String email) {
        bindAndStart(email);
    }

    /** "Apagar deste celular": fica só o que está na conta (baixado na próxima rodada). */
    @WorkerThread
    public void replaceLocalData(String email) {
        wipeLocalData();
        prefs.clear();
        bindAndStart(email);
    }

    /** Desistiu de entrar (dados de outra conta no aparelho): a sessão nova é encerrada. */
    @WorkerThread
    public void cancelSignIn() {
        account.signOut();
    }

    /** Conta à qual os dados do aparelho pertencem (nula = nenhuma). */
    public String boundEmail() {
        return prefs.boundEmail();
    }

    @WorkerThread
    public int pendingCount() {
        return sync.pendingCount();
    }

    /**
     * Sair: tenta mandar o que falta antes; depois encerra a sessão. {@code keepData} falso apaga
     * os dados do aparelho (eles continuam na conta, se foram enviados).
     */
    @WorkerThread
    public void signOut(boolean keepData) {
        engine.run();
        scheduler.cancelAll();
        account.signOut();
        if (!keepData) {
            wipeLocalData();
            prefs.clear();
        }
    }

    private void bindAndStart(String email) {
        prefs.bind(email);
        scheduler.schedulePeriodic();
        scheduler.requestSoon();
    }

    private void wipeLocalData() {
        database.runInTransaction(() -> {
            sync.deleteAllEntries();
            sync.deleteAllMeals();
            sync.deleteAllGoals();
            sync.deleteAllCustomFoods();
        });
        catalog.invalidateSearch();
    }
}
