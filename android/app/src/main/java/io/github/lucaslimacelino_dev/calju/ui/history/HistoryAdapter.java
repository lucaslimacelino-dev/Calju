package io.github.lucaslimacelino_dev.calju.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.function.Consumer;

import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.ItemHistoryDayBinding;
import io.github.lucaslimacelino_dev.calju.domain.NutrientTotal;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.ui.Dates;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

public class HistoryAdapter extends ListAdapter<HistoryDay, HistoryAdapter.ViewHolder> {

    private final LocalDate today;
    private final Consumer<LocalDate> onClick;

    public HistoryAdapter(LocalDate today, Consumer<LocalDate> onClick) {
        super(DIFF);
        this.today = today;
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemHistoryDayBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryDay day = getItem(position);
        Context context = holder.binding.getRoot().getContext();
        String title = Dates.shortTitle(context, day.date(), today);
        holder.binding.date.setText(title);

        String kcal = mark(context, day.kcal(), NutrientUnit.KCAL);
        if (day.goal() == null) {
            holder.binding.kcal.setText(context.getString(R.string.history_kcal, kcal));
            holder.binding.bar.setVisibility(View.GONE);
        } else {
            holder.binding.kcal.setText(context.getString(R.string.history_kcal_goal, kcal,
                    Formats.amount(day.goal().kcal(), NutrientUnit.KCAL)));
            holder.binding.bar.setVisibility(View.VISIBLE);
            holder.binding.bar.setProgress(Math.min(100,
                    Math.round(day.kcal().amount().floatValue() / day.goal().kcal().floatValue() * 100)));
        }
        holder.binding.details.setText(context.getString(R.string.history_macros,
                mark(context, day.protein(), NutrientUnit.GRAM),
                mark(context, day.carbohydrate(), NutrientUnit.GRAM),
                mark(context, day.fat(), NutrientUnit.GRAM)));
        holder.binding.getRoot().setOnClickListener(v -> onClick.accept(day.date()));
        // O TalkBack anuncia "toque duas vezes para abrir ... no diário".
        ViewCompat.replaceAccessibilityAction(holder.binding.getRoot(),
                AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                context.getString(R.string.history_open_day, title), null);
    }

    /** "≥ X" quando algum alimento do dia não tem o valor medido. */
    private static String mark(Context context, NutrientTotal total, NutrientUnit unit) {
        String value = Formats.amount(total.amount(), unit);
        return total.complete() ? value : context.getString(R.string.value_at_least, value);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemHistoryDayBinding binding;

        ViewHolder(ItemHistoryDayBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<HistoryDay> DIFF = new DiffUtil.ItemCallback<HistoryDay>() {
        @Override
        public boolean areItemsTheSame(@NonNull HistoryDay a, @NonNull HistoryDay b) {
            return a.date().equals(b.date());
        }

        @Override
        public boolean areContentsTheSame(@NonNull HistoryDay a, @NonNull HistoryDay b) {
            return a.equals(b);
        }
    };
}
