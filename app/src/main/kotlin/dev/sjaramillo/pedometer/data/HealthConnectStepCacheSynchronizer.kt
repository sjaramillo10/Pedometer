package dev.sjaramillo.pedometer.data

import androidx.room.withTransaction
import java.time.LocalDate
import javax.inject.Inject

class HealthConnectStepCacheSynchronizer @Inject constructor(
    private val database: PedometerDatabase,
    private val phoneStepsDataSource: PhoneStepsDataSource,
) {
    suspend fun sync() {
        val today = LocalDate.now()
        val start = today.minusDays(RECENT_HISTORY_DAYS - 1)
        val phoneStepsByDay = phoneStepsDataSource.readDailySteps(start, today)

        database.withTransaction {
            val dailyStepsDao = database.dailyStepsDao()
            val cacheEntries =
                mergeRecentSteps(
                    start = start,
                    end = today,
                    healthConnectStepsByDay = phoneStepsByDay,
                    cachedSteps = dailyStepsDao.getEntriesInRange(start.toEpochDay(), today.toEpochDay()),
                )
            dailyStepsDao.deleteFrom(start.toEpochDay(), today.toEpochDay())
            dailyStepsDao.insertAll(cacheEntries)
        }
    }

    private companion object {
        const val RECENT_HISTORY_DAYS = 30L
    }
}
