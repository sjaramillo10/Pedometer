package dev.sjaramillo.nemi.ui

import dev.sjaramillo.nemi.R

enum class AppTheme(val preferenceValue: String, val label: Int) {
    SYSTEM("system", R.string.theme_system),
    LIGHT("light", R.string.theme_light),
    DARK("dark", R.string.theme_dark),
    ;

    internal fun isDark(systemDark: Boolean): Boolean =
        when (this) {
            SYSTEM -> systemDark
            LIGHT -> false
            DARK -> true
        }

    companion object {
        internal fun fromPreference(value: String?): AppTheme = entries.firstOrNull { it.preferenceValue == value } ?: SYSTEM
    }
}
