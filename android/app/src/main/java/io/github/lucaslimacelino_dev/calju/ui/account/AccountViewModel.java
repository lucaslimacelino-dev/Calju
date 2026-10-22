package io.github.lucaslimacelino_dev.calju.ui.account;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.concurrent.Executor;
import java.util.function.Supplier;

import io.github.lucaslimacelino_dev.calju.AppContainer;
import io.github.lucaslimacelino_dev.calju.data.repository.AccountRepository;

/**
 * Passos da folha de conta: e-mail → (senha | código + senha nova). O código serve para criar a
 * conta e para redefinir a senha. As regras de verdade ficam no servidor; aqui só se evita mandar
 * algo que com certeza seria recusado.
 */
public class AccountViewModel extends ViewModel {

    public enum Step {
        EMAIL,
        PASSWORD,
        CODE
    }

    public enum CodeFor {
        SIGN_UP,
        RESET
    }

    static final int MIN_PASSWORD = 12;

    private final AccountRepository account;
    private final Executor executor;
    private final MutableLiveData<Step> step = new MutableLiveData<>(Step.EMAIL);
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> done = new MutableLiveData<>(false);
    private CodeFor codeFor = CodeFor.SIGN_UP;
    private String email = "";

    public AccountViewModel(AccountRepository account, Executor executor) {
        this.account = account;
        this.executor = executor;
    }

    public LiveData<Step> step() {
        return step;
    }

    public LiveData<Boolean> busy() {
        return busy;
    }

    /** Mensagem do servidor (ou de rede) para mostrar; nula = sem erro. */
    public LiveData<String> error() {
        return error;
    }

    public LiveData<Boolean> done() {
        return done;
    }

    public CodeFor codeFor() {
        return codeFor;
    }

    public String email() {
        return email;
    }

    public void usePassword(String typedEmail) {
        email = typedEmail.trim();
        error.setValue(null);
        step.setValue(Step.PASSWORD);
    }

    public void createAccount(String typedEmail) {
        email = typedEmail.trim();
        codeFor = CodeFor.SIGN_UP;
        run(() -> account.requestSignUpCode(email), () -> step.postValue(Step.CODE));
    }

    public void forgotPassword() {
        codeFor = CodeFor.RESET;
        run(() -> account.requestPasswordReset(email), () -> step.postValue(Step.CODE));
    }

    public void resendCode() {
        run(() -> codeFor == CodeFor.SIGN_UP ? account.requestSignUpCode(email) : account.requestPasswordReset(email),
                () -> { });
    }

    public void login(String password) {
        run(() -> account.login(email, password), () -> done.postValue(true));
    }

    public void confirmCode(String code, String newPassword) {
        run(() -> codeFor == CodeFor.SIGN_UP
                        ? account.confirmSignUp(email, code, newPassword)
                        : account.confirmPasswordReset(email, code, newPassword),
                () -> done.postValue(true));
    }

    public void back() {
        error.setValue(null);
        step.setValue(Step.EMAIL);
    }

    public void signOut() {
        executor.execute(account::signOut);
    }

    private void run(Supplier<AccountRepository.Result> action, Runnable onSuccess) {
        if (Boolean.TRUE.equals(busy.getValue())) {
            return;
        }
        busy.setValue(true);
        error.setValue(null);
        executor.execute(() -> {
            AccountRepository.Result result = action.get();
            busy.postValue(false);
            if (result.ok()) {
                onSuccess.run();
            } else {
                error.postValue(result.error());
            }
        });
    }

    public static ViewModelProvider.Factory factory(AppContainer container) {
        return new ViewModelProvider.Factory() {
            @NonNull
            @Override
            @SuppressWarnings("unchecked")
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                return (T) new AccountViewModel(container.accountRepository, container.networkExecutor);
            }
        };
    }
}
