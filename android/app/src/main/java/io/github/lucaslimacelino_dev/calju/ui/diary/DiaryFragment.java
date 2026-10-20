package io.github.lucaslimacelino_dev.calju.ui.diary;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import java.math.BigDecimal;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.CaLjuApp;
import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.FragmentDiaryBinding;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

public class DiaryFragment extends Fragment {

    private FragmentDiaryBinding binding;
    private DiaryViewModel viewModel;
    private SuggestionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentDiaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        viewModel = new ViewModelProvider(this,
                DiaryViewModel.factory(((CaLjuApp) requireActivity().getApplication()).container()))
                .get(DiaryViewModel.class);

        adapter = new SuggestionAdapter(viewModel::select);
        binding.suggestions.setAdapter(adapter);
        viewModel.suggestions().observe(getViewLifecycleOwner(), this::showSuggestions);
        viewModel.selected().observe(getViewLifecycleOwner(), selected -> {
            if (selected != null && getChildFragmentManager().findFragmentByTag(QuantityInputSheet.TAG) == null) {
                new QuantityInputSheet().show(getChildFragmentManager(), QuantityInputSheet.TAG);
            }
        });

        binding.foodInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                viewModel.onTextChanged(s.toString());
            }
        });
        binding.foodInput.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                selectFirstSuggestion();
                return true;
            }
            return false;
        });
        binding.sendButton.setOnClickListener(v -> selectFirstSuggestion());

        getChildFragmentManager().setFragmentResultListener(QuantityInputSheet.RESULT_KEY, getViewLifecycleOwner(),
                (key, result) -> onQuantityConfirmed(result));
    }

    private void showSuggestions(List<Suggestion> suggestions) {
        adapter.submitList(suggestions);
        boolean visible = !suggestions.isEmpty();
        binding.suggestionsCard.setVisibility(visible ? View.VISIBLE : View.GONE);
        binding.emptyState.setVisibility(visible ? View.GONE : View.VISIBLE);
    }

    private void selectFirstSuggestion() {
        if (adapter.getItemCount() > 0) {
            viewModel.select(adapter.getCurrentList().get(0));
        }
    }

    /** Fase 4a: o registro ainda não é gravado; a Fase 4b grava no diário. */
    private void onQuantityConfirmed(Bundle result) {
        String name = result.getString(QuantityInputSheet.RESULT_NAME);
        BigDecimal grams = new BigDecimal(result.getString(QuantityInputSheet.RESULT_GRAMS));
        binding.foodInput.setText("");
        viewModel.clearSuggestions();
        // Ao fechar, o painel devolve o foco ao campo; o teclado é fechado depois disso.
        binding.getRoot().post(this::hideKeyboard);
        Snackbar.make(binding.getRoot(), getString(R.string.add_pending, name, Formats.grams(grams)),
                        Snackbar.LENGTH_LONG)
                .setAnchorView(binding.composer)
                .show();
    }

    private void hideKeyboard() {
        if (binding == null) {
            return;
        }
        binding.foodInput.clearFocus();
        WindowCompat.getInsetsController(requireActivity().getWindow(), binding.getRoot())
                .hide(WindowInsetsCompat.Type.ime());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
