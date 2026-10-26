package io.github.lucaslimacelino_dev.calju;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import io.github.lucaslimacelino_dev.calju.databinding.ActivityWelcomeBinding;
import io.github.lucaslimacelino_dev.calju.ui.goal.GoalQuizActivity;

/**
 * Porta de entrada do app. Na primeira vez, o Caju se apresenta em duas páginas e oferece o
 * questionário da meta; depois disso esta tela só encaminha direto para o Diário.
 */
public class WelcomeActivity extends AppCompatActivity {

    private static final String PREFS = "calju";
    private static final String KEY_SEEN = "welcome_seen";
    private static final String STATE_PAGE = "page";

    private ActivityWelcomeBinding binding;
    private int page;

    public static boolean wasSeen(Context context) {
        return context.getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(KEY_SEEN, false);
    }

    public static void markSeen(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_SEEN, true).apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (wasSeen(this)) {
            super.onCreate(savedInstanceState);
            openDiary();
            return;
        }
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        binding = ActivityWelcomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        ViewCompat.setOnApplyWindowInsetsListener(binding.welcomeRoot, (root, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            root.setPadding(root.getPaddingLeft(), bars.top, root.getPaddingRight(),
                    bars.bottom + getResources().getDimensionPixelSize(R.dimen.welcome_bottom_padding));
            return insets;
        });

        page = savedInstanceState == null ? 0 : savedInstanceState.getInt(STATE_PAGE);
        binding.skip.setOnClickListener(v -> finishWelcome());
        binding.next.setOnClickListener(v -> {
            if (page == 0) {
                page = 1;
                showPage();
            } else {
                finishWelcome();
            }
        });
        showPage();
    }

    private void showPage() {
        binding.pageHello.setVisibility(page == 0 ? View.VISIBLE : View.GONE);
        binding.pageHow.setVisibility(page == 1 ? View.VISIBLE : View.GONE);
        binding.next.setText(page == 0 ? R.string.welcome_start : R.string.welcome_done);
        binding.pageIndicator.setText(getString(R.string.welcome_page, page + 1, 2));
    }

    private void finishWelcome() {
        markSeen(this);
        startActivity(GoalQuizActivity.onboarding(this));
        finish();
    }

    private void openDiary() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_PAGE, page);
    }
}
