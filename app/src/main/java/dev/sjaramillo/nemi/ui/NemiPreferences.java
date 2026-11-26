package dev.sjaramillo.nemi.ui;

import android.content.SharedPreferences;
import dev.sjaramillo.nemi.State;

public final class NemiPreferences {
    static final int DEFAULT_GOAL = 10_000;
    private final SharedPreferences preferences;
    final State<Integer> goal;
    final State<AppTheme> theme;

    public NemiPreferences(SharedPreferences preferences) {
        this.preferences = preferences;
        goal = new State<>(preferences.getInt("goal", DEFAULT_GOAL));
        theme = new State<>(AppTheme.fromPreference(preferences.getString("theme", null)));
    }

    void adjustGoal(int amount) {
        int value = adjustGoal(goal.value(), amount);
        if (value != goal.value()) {
            preferences.edit().putInt("goal", value).apply();
            goal.set(value);
        }
    }

    void setTheme(AppTheme value) {
        preferences.edit().putString("theme", value.preferenceValue()).apply();
        theme.set(value);
    }

    static int adjustGoal(int goal, int amount) {
        return Math.max(1, Math.min(100_000, goal + amount));
    }
}
