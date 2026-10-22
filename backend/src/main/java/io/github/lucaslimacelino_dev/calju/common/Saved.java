package io.github.lucaslimacelino_dev.calju.common;

/** Resultado de um "criar ou atualizar": o controller responde 201 se criou, 200 se atualizou. */
public record Saved<T>(T value, boolean created) {
}
