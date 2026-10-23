package io.github.lucaslimacelino_dev.calju.ui.diary;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Objects;
import java.util.function.Consumer;

import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.ItemSuggestionBinding;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

/** Lista de sugestões. ListAdapter + DiffUtil atualizam só os itens que mudaram. */
public class SuggestionAdapter extends ListAdapter<Suggestion, SuggestionAdapter.ViewHolder> {

    private final Consumer<Suggestion> onClick;

    public SuggestionAdapter(Consumer<Suggestion> onClick) {
        super(DIFF);
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemSuggestionBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Suggestion suggestion = getItem(position);
        holder.binding.name.setText(suggestion.name());
        holder.binding.detail.setText(detail(holder.binding.getRoot().getContext(), suggestion));
        holder.binding.getRoot().setOnClickListener(v -> onClick.accept(suggestion));
    }

    static String detail(Context context, Suggestion s) {
        if (s.grams() == null) {
            return s.kcalPer100g() == null
                    ? context.getString(R.string.suggestion_ask_quantity)
                    : context.getString(R.string.suggestion_per_100g, Formats.amount(s.kcalPer100g(), NutrientUnit.KCAL),
                    s.unit().symbol());
        }
        String grams = Formats.quantity(s.grams(), s.unit());
        String quantity = s.portionText() == null ? grams : s.portionText() + " · " + grams;
        return s.kcal() == null
                ? quantity
                : context.getString(R.string.suggestion_with_kcal, quantity, Formats.amount(s.kcal(), NutrientUnit.KCAL));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemSuggestionBinding binding;

        ViewHolder(ItemSuggestionBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<Suggestion> DIFF = new DiffUtil.ItemCallback<Suggestion>() {
        @Override
        public boolean areItemsTheSame(@NonNull Suggestion a, @NonNull Suggestion b) {
            return a.foodId().equals(b.foodId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Suggestion a, @NonNull Suggestion b) {
            return Objects.equals(a, b);
        }
    };
}
