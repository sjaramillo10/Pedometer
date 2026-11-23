package dev.sjaramillo.pedometer.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class StepsRepository @Inject constructor(
    private val database: PedometerDatabase,
) {
    private val dailyStepsDao = database.dailyStepsDao()

    suspend fun getAll(): List<DailySteps> = resolveDailySteps(dailyStepsDao.getAll())

    fun observeAll(): Flow<List<DailySteps>> = dailyStepsDao.observeAll().resolveDailySteps()

    fun observeRecentDays(today: LocalDate): Flow<List<DailySteps>> =
        dailyStepsDao
            .observeEntriesInRange(today.minusDays(7).toEpochDay(), today.toEpochDay())
            .resolveDailySteps()

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

internal fun resolveDailySteps(entries: List<StoredDailySteps>): List<DailySteps> =
    entries
        .groupBy(StoredDailySteps::day)
        .map { (day, totals) ->
            val preferred = totals.firstOrNull { it.source == StepSource.HEALTH_CONNECT } ?: totals.first()
            DailySteps(day, preferred.steps, fromBackup = preferred.source == StepSource.BACKUP)
        }

internal fun Flow<List<StoredDailySteps>>.resolveDailySteps(): Flow<List<DailySteps>> = map(::resolveDailySteps)
