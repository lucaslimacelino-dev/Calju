package io.github.lucaslimacelino_dev.calju.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import com.google.android.material.color.MaterialColors;

/** Anel de progresso do dia (0 a 1). A animação respeita a opção do sistema de remover animações. */
public class DayRingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private float progress;
    private ValueAnimator animator;

    public DayRingView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        float stroke = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14, getResources().getDisplayMetrics());
        for (Paint paint : new Paint[]{track, progressPaint}) {
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(stroke);
            paint.setStrokeCap(Paint.Cap.ROUND);
        }
        track.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceContainer));
        progressPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary));
    }

    public void setProgress(float target) {
        float clamped = Math.max(0f, Math.min(1f, target));
        if (animator != null) {
            animator.cancel();
        }
        if (!ValueAnimator.areAnimatorsEnabled()) {
            progress = clamped;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(progress, clamped);
        animator.setDuration(700);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            progress = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float inset = track.getStrokeWidth() / 2f;
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f + inset;
        float top = (getHeight() - size) / 2f + inset;
        bounds.set(left, top, left + size - 2 * inset, top + size - 2 * inset);
        canvas.drawArc(bounds, 0, 360, false, track);
        if (progress > 0f) {
            canvas.drawArc(bounds, -90, 360 * progress, false, progressPaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (animator != null) {
            animator.cancel();
        }
        super.onDetachedFromWindow();
    }
}
