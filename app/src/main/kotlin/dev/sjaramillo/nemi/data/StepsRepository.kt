package dev.sjaramillo.nemi.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class StepsRepository(
    private val database: NemiDatabase,
) {
    private val dailyStepsDao = database.dailyStepsDao()

    suspend fun getAll(): List<DailySteps> = dailyStepsDao.getResolvedDays(1, lastSupportedEpochDay)

    fun observeDay(day: LocalDate): Flow<DailySteps?> = observeDays(day, day).map { it.singleOrNull() }

    fun observeDays(
        start: LocalDate,
        end: LocalDate,
    ): Flow<List<DailySteps>> =
        dailyStepsDao.observeResolvedDays(start.toEpochDay().coerceAtLeast(1), end.toEpochDay()).distinctUntilChanged()

    fun observeHistoryStats(): Flow<StepHistoryStats> = dailyStepsDao.observeHistoryStats(1, lastSupportedEpochDay).distinctUntilChanged()

    suspend fun importDailySteps(dailySteps: List<DailySteps>) {
        dailyStepsDao.insertAll(dailySteps.map { StoredDailySteps(it.day, it.steps, StepSource.BACKUP) })
    }

    internal suspend fun replaceHealthConnectWindow(
        start: LocalDate,
        end: LocalDate,
        totals: Map<LocalDate, Long>,
    ) {
        val entries = totals.map { (day, steps) -> StoredDailySteps(day.toEpochDay(), steps, StepSource.HEALTH_CONNECT) }
        database.withTransaction {
            dailyStepsDao.deleteFrom(StepSource.HEALTH_CONNECT, start.toEpochDay(), end.toEpochDay())
            dailyStepsDao.insertAll(entries)
        }
    }
}
