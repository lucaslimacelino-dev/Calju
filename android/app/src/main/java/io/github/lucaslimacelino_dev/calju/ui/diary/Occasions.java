package io.github.lucaslimacelino_dev.calju.ui.diary;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;

import io.github.lucaslimacelino_dev.calju.R;
import io.github.lucaslimacelino_dev.calju.domain.Occasion;

/** Liga cada ocasião ao seu texto e ao chip da tela de quantidade. */
final class Occasions {

    private Occasions() {
    }

    @StringRes
    static int label(Occasion occasion) {
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
