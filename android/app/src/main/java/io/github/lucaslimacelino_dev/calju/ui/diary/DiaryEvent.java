package io.github.lucaslimacelino_dev.calju.ui.diary;

import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/**
 * Aviso de uso único para a tela (ex.: "Adicionado ao almoço · Desfazer"). Fica numa LiveData,
 * e {@link #consume()} garante que não reapareça ao girar a tela.
 */
public final class DiaryEvent {

    public enum Type { ADDED, REMOVED }

    private final Type type;
    private final String entryId;
    private final Occasion occasion;
    private boolean consumed;

    public DiaryEvent(Type type, String entryId, Occasion occasion) {
        this.type = type;
        this.entryId = entryId;
        this.occasion = occasion;
    }

    /** Retorna verdadeiro só na primeira chamada. */
    public boolean consume() {
        if (consumed) {
            return false;
        }
        consumed = true;
        return true;
    }

    public Type type() {
        return type;
    }

    public String entryId() {
        return entryId;
    }

    public Occasion occasion() {
        return occasion;
    }
}
