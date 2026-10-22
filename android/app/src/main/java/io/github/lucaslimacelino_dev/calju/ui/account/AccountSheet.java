package io.github.lucaslimacelino_dev.calju.ui.account;

import android.app.Dialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.function.Consumer;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.textfield.TextInputEditText;

import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.SheetAccountBinding;

/** Entrar, criar conta (com código por e-mail) ou redefinir a senha. Avisa o Perfil ao concluir. */
public class AccountSheet extends BottomSheetDialogFragment {

    public static final String TAG = "AccountSheet";
    public static final String RESULT_KEY = "account";

    private SheetAccountBinding binding;
    private AccountViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = SheetAccountBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this,
                AccountViewModel.factory(((CaLjuApp) requireActivity().getApplication()).container()))
                .get(AccountViewModel.class);
        viewModel.step().observe(getViewLifecycleOwner(), this::showStep);
        viewModel.busy().observe(getViewLifecycleOwner(), busy -> {
            boolean on = Boolean.TRUE.equals(busy);
            binding.progress.setVisibility(on ? View.VISIBLE : View.GONE);
            binding.primaryButton.setEnabled(!on);
            binding.secondaryButton.setEnabled(!on);
            binding.linkButton.setEnabled(!on);
        });
        viewModel.error().observe(getViewLifecycleOwner(), error -> {
            binding.error.setVisibility(error == null ? View.GONE : View.VISIBLE);
            binding.error.setText(error);
        });
        viewModel.done().observe(getViewLifecycleOwner(), done -> {
            if (Boolean.TRUE.equals(done)) {
                getParentFragmentManager().setFragmentResult(RESULT_KEY, new Bundle());
                dismiss();
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog instanceof BottomSheetDialog) {
            BottomSheetBehavior<?> behavior = ((BottomSheetDialog) dialog).getBehavior();
            behavior.setSkipCollapsed(true);
            behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }
    }

    private void showStep(AccountViewModel.Step step) {
        binding.emailLayout.setVisibility(step == AccountViewModel.Step.EMAIL ? View.VISIBLE : View.GONE);
        binding.passwordLayout.setVisibility(step == AccountViewModel.Step.EMAIL ? View.GONE : View.VISIBLE);
        binding.codeLayout.setVisibility(step == AccountViewModel.Step.CODE ? View.VISIBLE : View.GONE);
        binding.passwordLayout.setError(null);
        binding.codeLayout.setError(null);
        binding.password.setText("");
        switch (step) {
            case EMAIL:
                binding.subtitle.setText(R.string.account_intro);
                binding.primaryButton.setText(R.string.account_create);
                binding.primaryButton.setOnClickListener(v -> withEmail(viewModel::createAccount));
                binding.secondaryButton.setVisibility(View.VISIBLE);
                binding.secondaryButton.setText(R.string.account_use_password);
                binding.secondaryButton.setOnClickListener(v -> withEmail(viewModel::usePassword));
                binding.linkButton.setVisibility(View.GONE);
                break;
            case PASSWORD:
                binding.subtitle.setText(viewModel.email());
                binding.passwordLayout.setHint(R.string.account_password);
                binding.primaryButton.setText(R.string.account_sign_in);
                binding.primaryButton.setOnClickListener(v -> {
                    String password = text(binding.password);
                    if (password.isEmpty()) {
                        binding.passwordLayout.setError(getString(R.string.account_password_required));
                        return;
                    }
                    hideKeyboard();
                    viewModel.login(password);
                });
                showBackAndLink(R.string.account_forgot, v -> viewModel.forgotPassword());
                break;
            default:
                boolean signUp = viewModel.codeFor() == AccountViewModel.CodeFor.SIGN_UP;
                binding.subtitle.setText(getString(signUp ? R.string.account_code_sent : R.string.account_reset_sent,
                        viewModel.email()));
                binding.passwordLayout.setHint(R.string.account_new_password);
                binding.code.setText("");
                binding.primaryButton.setText(signUp ? R.string.account_create : R.string.account_save_password);
                binding.primaryButton.setOnClickListener(v -> confirmCode());
                showBackAndLink(R.string.account_resend, v -> viewModel.resendCode());
                break;
        }
    }

    private void showBackAndLink(int linkText, View.OnClickListener linkAction) {
        binding.secondaryButton.setVisibility(View.VISIBLE);
        binding.secondaryButton.setText(R.string.account_back);
        binding.secondaryButton.setOnClickListener(v -> viewModel.back());
        binding.linkButton.setVisibility(View.VISIBLE);
        binding.linkButton.setText(linkText);
        binding.linkButton.setOnClickListener(linkAction);
    }

    private void withEmail(Consumer<String> action) {
        String email = text(binding.email).trim();
        if (TextUtils.isEmpty(email) || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailLayout.setError(getString(R.string.account_email_invalid));
            return;
        }
        binding.emailLayout.setError(null);
        hideKeyboard();
        action.accept(email);
    }

    private void confirmCode() {
        String code = text(binding.code).trim();
        String password = text(binding.password);
        binding.codeLayout.setError(code.matches("\\d{6}") ? null : getString(R.string.account_code_invalid));
        binding.passwordLayout.setError(password.length() >= AccountViewModel.MIN_PASSWORD ? null
                : getString(R.string.account_password_short));
        if (binding.codeLayout.getError() == null && binding.passwordLayout.getError() == null) {
            hideKeyboard();
            viewModel.confirmCode(code, password);
        }
    }

    private void hideKeyboard() {
        WindowCompat.getInsetsController(requireActivity().getWindow(), binding.getRoot())
                .hide(WindowInsetsCompat.Type.ime());
    }

    private static String text(TextInputEditText field) {
        return field.getText() == null ? "" : field.getText().toString();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
