package dev.sjaramillo.pedometer.data

import androidx.room.withTransaction
import java.time.LocalDate
import javax.inject.Inject

class HealthConnectStepCacheSynchronizer internal constructor(
    private val currentDate: () -> LocalDate,
    private val readDailySteps: suspend (LocalDate, LocalDate) -> Map<LocalDate, Long>,
    private val replaceWindow: suspend (Long, Long, List<StoredDailySteps>) -> Unit,
) {
    @Inject
    constructor(
        database: PedometerDatabase,
        phoneStepsDataSource: PhoneStepsDataSource,
    ) : this(LocalDate::now, phoneStepsDataSource::readDailySteps, { start, end, entries ->
        database.withTransaction {
            val dao = database.dailyStepsDao()
            dao.deleteFrom(StepSource.HEALTH_CONNECT, start, end)
            dao.insertAll(entries)
        }
    })

    suspend fun sync() {
        val today = currentDate()
        val start = today.minusDays(RECENT_HISTORY_DAYS - 1)
        val entries =
            readDailySteps(start, today).map { (day, steps) ->
                StoredDailySteps(day.toEpochDay(), steps, StepSource.HEALTH_CONNECT)
            }
        replaceWindow(start.toEpochDay(), today.toEpochDay(), entries)
    }

    private companion object {
        const val RECENT_HISTORY_DAYS = 30L
    }
}
