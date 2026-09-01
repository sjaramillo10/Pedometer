package dev.sjaramillo.pedometer.data

import androidx.room.withTransaction
import java.time.LocalDate
import javax.inject.Inject

class HealthConnectStepCacheSynchronizer @Inject constructor(
    private val database: PedometerDatabase,
    private val phoneStepsDataSource: PhoneStepsDataSource,
    private val syncPreferences: StepsSyncPreferences,
) {
    suspend fun sync() {
        val today = LocalDate.now()
        val start =
            syncPreferences.getStartDay() ?: today.minusDays(INITIAL_HISTORY_DAYS - 1).also {
                syncPreferences.setStartDay(it)
            }
        val phoneStepsByDay = phoneStepsDataSource.readDailySteps(start, today)
        val cacheEntries =
            generateSequence(start) { day -> day.plusDays(1).takeIf { it <= today } }
                .map { day -> DailySteps(day.toEpochDay(), phoneStepsByDay[day] ?: 0) }
                .toList()

        database.withTransaction {
            database.dailyStepsDao().deleteFrom(start.toEpochDay())
            database.dailyStepsDao().insertAll(cacheEntries)
        }
    }

    private companion object {
        const val INITIAL_HISTORY_DAYS = 30L
    }
}
