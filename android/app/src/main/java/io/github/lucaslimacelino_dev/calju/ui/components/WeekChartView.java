package io.github.lucaslimacelino_dev.calju.ui.components;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.color.MaterialColors;

import java.math.BigDecimal;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.domain.NutrientUnit;
import io.github.lucaslimacelino_dev.calju.ui.Formats;
import io.github.lucaslimacelino_dev.calju.ui.history.HistoryState.WeekBar;

/**
 * Barras de kcal dos 7 dias da semana com a meta como linha tracejada. Passar da meta não muda a
 * cor da barra: a linha já mostra, sem tom de alarme.
 */
public class WeekChartView extends View {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint emptyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint goalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint todayLabelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    /** Contorno da cor do cartão atrás dos números, para a linha da meta não riscar o texto. */
    private final Paint haloPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float dp;

    private List<WeekBar> bars = new ArrayList<>();
    private BigDecimal goal;
    private float grow = 1f;
    private ValueAnimator animator;

    public WeekChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getResources().getDisplayMetrics());
        float sp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1, getResources().getDisplayMetrics());
        int onSurface = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface);
        int variant = MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant);

        barPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSecondary));
        emptyPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOutline));
        goalPaint.setColor(MaterialColors.getColor(this, androidx.appcompat.R.attr.colorPrimary));
        goalPaint.setStyle(Paint.Style.STROKE);
        goalPaint.setStrokeWidth(1.5f * dp);
        goalPaint.setPathEffect(new DashPathEffect(new float[]{6 * dp, 4 * dp}, 0));

        Typeface typeface = ResourcesCompat.getFont(context, R.font.nunito);
        for (Paint paint : new Paint[]{labelPaint, todayLabelPaint, valuePaint}) {
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTypeface(typeface);
            paint.setTextSize(12 * sp);
        }
        labelPaint.setColor(variant);
        todayLabelPaint.setColor(onSurface);
        todayLabelPaint.setTypeface(Typeface.create(typeface, Typeface.BOLD));
        valuePaint.setColor(variant);
        valuePaint.setTextSize(11 * sp);
        haloPaint.set(valuePaint);
        haloPaint.setStyle(Paint.Style.STROKE);
        haloPaint.setStrokeWidth(4 * dp);
        haloPaint.setColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface));
    }

    public void setData(List<WeekBar> bars, @Nullable BigDecimal goal) {
        this.bars = bars;
        this.goal = goal;
        if (animator != null) {
            animator.cancel();
        }
        if (!ValueAnimator.areAnimatorsEnabled()) {
            grow = 1f;
            invalidate();
            return;
        }
        animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            grow = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (bars.isEmpty()) {
            return;
        }
        float valueArea = 18 * dp;
        float labelArea = 24 * dp;
        float top = getPaddingTop() + valueArea;
        float bottom = getHeight() - getPaddingBottom() - labelArea;
        float left = getPaddingLeft();
        float slot = (getWidth() - getPaddingLeft() - getPaddingRight()) / (float) bars.size();
        float barWidth = Math.min(slot * 0.45f, 24 * dp);
        float chartHeight = bottom - top;

        float max = goal == null ? 0f : goal.floatValue() * 1.1f;
        for (WeekBar bar : bars) {
            if (bar.kcal() != null) {
                max = Math.max(max, bar.kcal().floatValue());
            }
        }

        for (int i = 0; i < bars.size(); i++) {
            WeekBar bar = bars.get(i);
            float center = left + slot * i + slot / 2f;
            String weekday = bar.date().getDayOfWeek().getDisplayName(TextStyle.NARROW, PT_BR).toUpperCase(PT_BR);
            canvas.drawText(weekday, center, bottom + labelArea - 6 * dp, bar.today() ? todayLabelPaint : labelPaint);
            if (bar.future()) {
                continue;
            }
            if (bar.kcal() == null || max <= 0f) {
                rect.set(center - barWidth / 2f, bottom - 4 * dp, center + barWidth / 2f, bottom);
                canvas.drawRoundRect(rect, 2 * dp, 2 * dp, emptyPaint);
                continue;
            }
            float height = Math.max(4 * dp, chartHeight * bar.kcal().floatValue() / max * grow);
            rect.set(center - barWidth / 2f, bottom - height, center + barWidth / 2f, bottom);
            float radius = Math.min(barWidth / 2f, 8 * dp);
            canvas.drawRoundRect(rect, radius, radius, barPaint);
        }

        if (goal != null && max > 0f) {
            float y = bottom - chartHeight * goal.floatValue() / max;
            canvas.drawLine(left, y, getWidth() - getPaddingRight(), y, goalPaint);
        }

        if (grow < 1f) {
            return;
        }
        for (int i = 0; i < bars.size(); i++) {
            WeekBar bar = bars.get(i);
            if (bar.future() || bar.kcal() == null || max <= 0f) {
                continue;
            }
            float center = left + slot * i + slot / 2f;
            float height = Math.max(4 * dp, chartHeight * bar.kcal().floatValue() / max);
            String value = Formats.amount(bar.kcal(), NutrientUnit.KCAL);
            float y = bottom - height - 5 * dp;
            canvas.drawText(value, center, y, haloPaint);
            canvas.drawText(value, center, y, valuePaint);
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
