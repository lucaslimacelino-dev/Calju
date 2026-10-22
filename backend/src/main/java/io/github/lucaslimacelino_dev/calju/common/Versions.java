package io.github.lucaslimacelino_dev.calju.common;

/**
 * Concorrência otimista: o cliente informa a versão que conhece (0 = registro novo). O servidor
 * só aplica a mudança se ela for a versão atual; senão responde 409 com a versão atual.
 */
public final class Versions {

    public static final int NEW = 0;

    private Versions() {
    }

    public static void requireCurrent(int expected, int current) {
        if (expected != current) {
            throw new ConflictException("O registro mudou desde a última leitura (versão atual: " + current + ").",
                    current);
        }
    }
}
