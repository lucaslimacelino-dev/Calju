package io.github.lucaslimacelino_dev.calju.ui.components;

import android.animation.ValueAnimator;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.function.Function;

import io.github.lucaslimacelino_dev.calju.R;

/**
 * Anima um número num TextView do valor anterior até o novo (efeito "contador").
 * O texto final é sempre o valor exato formatado; a animação é só visual.
 */
public final class NumberAnimator {

    private static final long DURATION_MS = 700;

    private NumberAnimator() {
    }

    public static void animate(TextView view, BigDecimal target, Function<BigDecimal, String> format) {
        Object running = view.getTag(R.id.tag_number_animator);
        if (running instanceof ValueAnimator) {
            ((ValueAnimator) running).cancel();
        }
        Object last = view.getTag(R.id.tag_number_value);
        BigDecimal from = last instanceof BigDecimal ? (BigDecimal) last : BigDecimal.ZERO;
        view.setTag(R.id.tag_number_value, target);

        String finalText = format.apply(target);
        if (!ValueAnimator.areAnimatorsEnabled() || from.compareTo(target) == 0) {
            view.setText(finalText);
            return;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(from.floatValue(), target.floatValue());
        animator.setDuration(DURATION_MS);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            if (a.getAnimatedFraction() >= 1f) {
                view.setText(finalText);
            } else {
                view.setText(format.apply(BigDecimal.valueOf((float) a.getAnimatedValue())));
            }
        });
        view.setTag(R.id.tag_number_animator, animator);
        animator.start();
    }
}
