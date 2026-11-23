package dev.sjaramillo.pedometer.data

import java.time.LocalDate
import javax.inject.Inject

class StepsRepository @Inject constructor(
    database: PedometerDatabase,
) {
    private val dailyStepsDao = database.dailyStepsDao()

    suspend fun getAll(): List<DailySteps> = resolveDailySteps(dailyStepsDao.getAll())

    suspend fun getRecentDays(today: LocalDate): List<DailySteps> =
        resolveDailySteps(dailyStepsDao.getEntriesInRange(today.minusDays(7).toEpochDay(), today.toEpochDay()))

    suspend fun importDailySteps(dailySteps: List<DailySteps>) {
        dailyStepsDao.insertAll(dailySteps.map { StoredDailySteps(it.day, it.steps, StepSource.CSV) })
    }
}

internal fun resolveDailySteps(entries: List<StoredDailySteps>): List<DailySteps> =
    entries
        .groupBy(StoredDailySteps::day)
        .map { (day, totals) ->
            val preferred = totals.firstOrNull { it.source == StepSource.HEALTH_CONNECT } ?: totals.first()
            DailySteps(day, preferred.steps)
        }
