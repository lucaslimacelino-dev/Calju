package io.github.lucaslimacelino_dev.calju.data.local;

import android.content.Context;
import android.content.SharedPreferences;

import java.math.BigDecimal;

import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Activity;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Objective;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Pace;
import io.github.lucaslimacelino_dev.calju.domain.GoalCalculator.Sex;
import io.github.lucaslimacelino_dev.calju.domain.UserProfile;

/** Respostas do questionário, só neste aparelho (não vão para o servidor). */
public class ProfileStore {

    private static final String PREFS = "calju_profile";
    private static final String SEX = "sex";
    private static final String BIRTH_YEAR = "birth_year";
    private static final String HEIGHT = "height_cm";
    private static final String WEIGHT = "weight_kg";
    private static final String ACTIVITY = "activity";
    private static final String OBJECTIVE = "objective";
    private static final String PACE = "pace";
    private static final String SPECIAL = "special_condition";

    private final SharedPreferences prefs;

    public ProfileStore(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void save(UserProfile profile) {
        prefs.edit()
                .putString(SEX, profile.sex().name())
                .putInt(BIRTH_YEAR, profile.birthYear())
                .putString(HEIGHT, profile.heightCm().toPlainString())
                .putString(WEIGHT, profile.weightKg().toPlainString())
                .putString(ACTIVITY, profile.activity() == null ? null : profile.activity().name())
                .putString(OBJECTIVE, profile.objective().name())
                .putString(PACE, profile.pace() == null ? null : profile.pace().name())
                .putBoolean(SPECIAL, profile.specialCondition())
                .apply();
    }

    /** Nulo se o questionário ainda não foi respondido (ou os dados foram apagados). */
    public UserProfile load() {
        String sex = prefs.getString(SEX, null);
        if (sex == null) {
            return null;
        }
        String activity = prefs.getString(ACTIVITY, null);
        String pace = prefs.getString(PACE, null);
        return new UserProfile(Sex.valueOf(sex), prefs.getInt(BIRTH_YEAR, 0),
                new BigDecimal(prefs.getString(HEIGHT, "0")), new BigDecimal(prefs.getString(WEIGHT, "0")),
                activity == null ? null : Activity.valueOf(activity),
                Objective.valueOf(prefs.getString(OBJECTIVE, Objective.KEEP.name())),
                pace == null ? null : Pace.valueOf(pace), prefs.getBoolean(SPECIAL, false));
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
