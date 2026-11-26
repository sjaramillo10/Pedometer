package dev.sjaramillo.nemi.ui

import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class NemiPreferences(private val preferences: SharedPreferences) {
    private val currentGoal = MutableStateFlow(preferences.getInt(GOAL_KEY, DEFAULT_GOAL))
    private val currentTheme = MutableStateFlow(AppTheme.fromPreference(preferences.getString(THEME_KEY, null)))

    val goal = currentGoal.asStateFlow()
    val theme = currentTheme.asStateFlow()

    internal fun adjustGoal(amount: Int) {
        val value = adjustGoal(currentGoal.value, amount)
        if (value == currentGoal.value) return
        preferences.edit().putInt(GOAL_KEY, value).apply()
        currentGoal.value = value
    }

    internal fun setTheme(value: AppTheme) {
        preferences.edit().putString(THEME_KEY, value.preferenceValue).apply()
        currentTheme.value = value
    }
}

private const val GOAL_KEY = "goal"
private const val THEME_KEY = "theme"
internal const val DEFAULT_GOAL = 10_000
private val goalRange = 1..100_000

internal fun adjustGoal(
    goal: Int,
    amount: Int,
): Int = (goal + amount).coerceIn(goalRange)
