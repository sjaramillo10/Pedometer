package dev.sjaramillo.pedometer.data

import android.content.SharedPreferences
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class DebugStepsProvider @Inject constructor(
    private val preferences: SharedPreferences,
    private val phoneProvider: Provider<PhoneStepsDataSource>,
    private val fakeProvider: FakeStepsProvider,
) : StepsProvider {
    override val isFake: Boolean get() = preferences.getBoolean(FAKE_STEPS_KEY, false)

    fun setFakeSteps(enabled: Boolean) {
        preferences.edit().putBoolean(FAKE_STEPS_KEY, enabled).apply()
    }

    private fun selected(): StepsProvider = if (isFake) fakeProvider else phoneProvider.get()

    override suspend fun access(): StepsAccess = selected().access()

    override suspend fun readDailySteps(
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Long> = selected().readDailySteps(start, end)

    private companion object {
        const val FAKE_STEPS_KEY = "debug_fake_steps"
    }
}
