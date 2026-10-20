package io.github.lucaslimacelino_dev.calju.ui.diary;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.function.Consumer;

import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.databinding.ItemLogEntryBinding;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.ui.Formats;

/** Cartões dos registros do dia: "ALMOÇO · 12:40", alimento, gramas e kcal. */
public class EntryAdapter extends ListAdapter<EntryRow, EntryAdapter.ViewHolder> {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final Consumer<EntryRow> onClick;

    public EntryAdapter(Consumer<EntryRow> onClick) {
        super(DIFF);
        this.onClick = onClick;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemLogEntryBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EntryRow row = getItem(position);
        Context context = holder.binding.getRoot().getContext();
        String time = TIME.format(Instant.ofEpochMilli(row.createdAt()).atZone(ZoneId.systemDefault()));
        holder.binding.tag.setText(context.getString(R.string.entry_tag,
                context.getString(Occasions.label(row.occasion())), time));
        holder.binding.kcal.setText(row.kcal() == null
                ? context.getString(R.string.value_unknown)
                : context.getString(R.string.entry_kcal, Formats.amount(row.kcal(), NutrientUnit.KCAL)));
        holder.binding.foodName.setText(row.foodName());
        holder.binding.grams.setText(Formats.grams(row.grams()));
        holder.binding.getRoot().setOnClickListener(v -> onClick.accept(row));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemLogEntryBinding binding;

        ViewHolder(ItemLogEntryBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }

    private static final DiffUtil.ItemCallback<EntryRow> DIFF = new DiffUtil.ItemCallback<EntryRow>() {
        @Override
        public boolean areItemsTheSame(@NonNull EntryRow a, @NonNull EntryRow b) {
            return a.itemId().equals(b.itemId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull EntryRow a, @NonNull EntryRow b) {
            return a.occasion().equals(b.occasion()) && a.grams().compareTo(b.grams()) == 0
                    && Objects.equals(a.kcal(), b.kcal()) && a.foodName().equals(b.foodName());
        }
    };
}
