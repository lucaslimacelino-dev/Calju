package io.github.lucaslimacelino_dev.calju.ui;

import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.View;
import android.view.ViewGroup;

import androidx.interpolator.view.animation.FastOutSlowInInterpolator;

/** Animações curtas de abrir e fechar, iguais em todo o app. */
public final class Motion {

    public static final long DURATION_MS = 200;

    private Motion() {
    }

    /** As próximas mudanças de visibilidade e tamanho dentro de {@code root} acontecem animadas. */
    public static void animateLayout(ViewGroup root) {
        TransitionManager.beginDelayedTransition(root, new AutoTransition()
                .setDuration(DURATION_MS)
                .setInterpolator(new FastOutSlowInInterpolator()));
    }

    /** Gira a seta de abrir/fechar: 180° aberta, 0° fechada. */
    public static void rotateChevron(View chevron, boolean open) {
        chevron.animate()
                .rotation(open ? 180f : 0f)
                .setDuration(DURATION_MS)
                .setInterpolator(new FastOutSlowInInterpolator())
                .start();
    }
}
