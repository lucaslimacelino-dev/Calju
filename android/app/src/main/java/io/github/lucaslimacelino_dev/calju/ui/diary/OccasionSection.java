package io.github.lucaslimacelino_dev.calju.ui.diary;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/**
 * Uma seção do Diário (café, almoço, jantar ou lanche) com seus registros. {@code kcal} soma só
 * os registros com energia conhecida; {@code complete} é falso se algum não tiver.
 */
public record OccasionSection(Occasion occasion, List<EntryRow> entries, BigDecimal kcal, boolean complete) {

    /** Sempre devolve as quatro ocasiões, na ordem do diário, mesmo as vazias. */
    public static List<OccasionSection> group(List<EntryRow> rows) {
        List<OccasionSection> sections = new ArrayList<>();
        for (Occasion occasion : Occasion.values()) {
            List<EntryRow> entries = new ArrayList<>();
            BigDecimal kcal = BigDecimal.ZERO;
            boolean complete = true;
            for (EntryRow row : rows) {
                if (row.occasion() != occasion) {
                    continue;
                }
                entries.add(row);
                if (row.kcal() == null) {
                    complete = false;
                } else {
                    kcal = kcal.add(row.kcal());
                }
            }
            sections.add(new OccasionSection(occasion, entries, kcal, complete));
        }
        return sections;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }
}
