package dev.sjaramillo.nemi.ui;

import dev.sjaramillo.nemi.R;

enum AppTheme {
    SYSTEM("system", R.string.theme_system),
    LIGHT("light", R.string.theme_light),
    DARK("dark", R.string.theme_dark);

    private final String preferenceValue;
    private final int label;

    AppTheme(String preferenceValue, int label) {
        this.preferenceValue = preferenceValue;
        this.label = label;
    }

    String preferenceValue() {
        return preferenceValue;
    }

    int label() {
        return label;
    }

    static AppTheme fromPreference(String preference) {
        for (AppTheme theme : values()) {
            if (theme.preferenceValue.equals(preference)) {
                return theme;
            }
        }
        return SYSTEM;
    }

    boolean isDark(boolean systemDark) {
        return this == DARK || (this == SYSTEM && systemDark);
    }
}
