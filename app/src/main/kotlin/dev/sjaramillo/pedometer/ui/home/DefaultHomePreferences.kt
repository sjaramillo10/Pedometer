package dev.sjaramillo.pedometer.ui.home

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sjaramillo.pedometer.ui.SettingsFragment
import javax.inject.Inject

class DefaultHomePreferences @Inject constructor(
    @ApplicationContext context: Context,
) : HomePreferences {
    private val preferences =
        context.getSharedPreferences("pedometer", Context.MODE_PRIVATE)

    override val goal: Int
        get() = preferences.getInt("goal", SettingsFragment.DEFAULT_GOAL)

    override val stepSize: Float
        get() = preferences.getFloat("step_size_value", SettingsFragment.DEFAULT_STEP_SIZE)

    override val stepSizeCm: Boolean
        get() =
            preferences.getString("step_size_unit", SettingsFragment.DEFAULT_STEP_UNIT) == "cm"
}
