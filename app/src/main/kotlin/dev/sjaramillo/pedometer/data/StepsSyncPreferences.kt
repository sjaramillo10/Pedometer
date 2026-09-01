package dev.sjaramillo.pedometer.data

import android.content.SharedPreferences
import java.time.LocalDate
import javax.inject.Inject

class StepsSyncPreferences @Inject constructor(
    private val preferences: SharedPreferences,
) {
    fun getStartDay(): LocalDate? =
        preferences
            .getString(KEY_SYNC_START_DAY, null)
            ?.let(LocalDate::parse)

    fun setStartDay(day: LocalDate) {
        preferences.edit().putString(KEY_SYNC_START_DAY, day.toString()).apply()
    }

    private companion object {
        const val KEY_SYNC_START_DAY = "health_connect_sync_start_day"
    }
}
