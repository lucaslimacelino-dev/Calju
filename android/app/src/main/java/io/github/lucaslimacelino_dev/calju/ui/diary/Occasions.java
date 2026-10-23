package io.github.lucaslimacelino_dev.calju.ui.diary;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/** Liga cada ocasião ao seu texto, ícone, cores e ao chip da tela de quantidade. */
public final class Occasions {

    private Occasions() {
    }

    @StringRes
    public static int label(Occasion occasion) {
        switch (occasion) {
            case BREAKFAST:
                return R.string.occasion_breakfast;
            case LUNCH:
                return R.string.occasion_lunch;
            case DINNER:
                return R.string.occasion_dinner;
            default:
                return R.string.occasion_snack;
        }
    }

    @DrawableRes
    static int icon(Occasion occasion) {
        switch (occasion) {
            case BREAKFAST:
                return R.drawable.ic_breakfast;
            case LUNCH:
                return R.drawable.ic_lunch;
            case DINNER:
                return R.drawable.ic_dinner;
            default:
                return R.drawable.ic_snack;
        }
    }

    @ColorRes
    static int color(Occasion occasion) {
        switch (occasion) {
            case BREAKFAST:
                return R.color.breakfast;
            case LUNCH:
                return R.color.lunch;
            case DINNER:
                return R.color.dinner;
            default:
                return R.color.snack;
        }
    }

    @ColorRes
    static int container(Occasion occasion) {
        switch (occasion) {
            case BREAKFAST:
                return R.color.breakfast_container;
            case LUNCH:
                return R.color.lunch_container;
            case DINNER:
                return R.color.dinner_container;
            default:
                return R.color.snack_container;
        }
    }

    @IdRes
    static int chip(Occasion occasion) {
        switch (occasion) {
            case BREAKFAST:
                return R.id.occasion_breakfast;
            case LUNCH:
                return R.id.occasion_lunch;
            case DINNER:
                return R.id.occasion_dinner;
            default:
                return R.id.occasion_snack;
        }
    }

    /** Os ids de R não são constantes de compilação no AGP 9, por isso if/else em vez de switch. */
    static Occasion fromChip(@IdRes int chipId) {
        if (chipId == R.id.occasion_breakfast) {
            return Occasion.BREAKFAST;
        }
        if (chipId == R.id.occasion_lunch) {
            return Occasion.LUNCH;
        }
        if (chipId == R.id.occasion_dinner) {
            return Occasion.DINNER;
        }
        return Occasion.SNACK;
    }
}
