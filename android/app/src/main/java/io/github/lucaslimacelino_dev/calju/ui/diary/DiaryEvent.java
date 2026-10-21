package io.github.lucaslimacelino_dev.calju.ui.diary;

import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/**
 * Aviso de uso único para a tela (ex.: "Adicionado ao almoço · Desfazer"). Fica numa LiveData,
 * e {@link #consume()} garante que não reapareça ao girar a tela.
 */
public final class DiaryEvent {

    public enum Type { ADDED, REMOVED, MEAL_SAVED, MEAL_DUPLICATED, MEAL_DELETED }

    private final Type type;
    private final String id;
    private final Occasion occasion;
    private final String name;
    private boolean consumed;

    public DiaryEvent(Type type, String id, Occasion occasion, String name) {
        this.type = type;
        this.id = id;
        this.occasion = occasion;
        this.name = name;
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

    /** Id do registro do diário (ADDED, REMOVED) ou da refeição salva (MEAL_*). */
    public String id() {
        return id;
    }

    public Occasion occasion() {
        return occasion;
    }

    public String name() {
        return name;
    }

    public boolean canUndo() {
        return type == Type.ADDED || type == Type.REMOVED || type == Type.MEAL_DELETED;
    }
}
