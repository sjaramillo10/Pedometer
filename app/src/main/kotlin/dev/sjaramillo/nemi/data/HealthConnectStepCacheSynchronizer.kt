package dev.sjaramillo.nemi.data

import java.time.LocalDate

class HealthConnectStepCacheSynchronizer internal constructor(
    private val currentDate: () -> LocalDate,
    private val readDailySteps: suspend (LocalDate, LocalDate) -> Map<LocalDate, Long>,
    private val replaceWindow: suspend (LocalDate, LocalDate, Map<LocalDate, Long>) -> Unit,
) {
    constructor(
        stepsRepository: StepsRepository,
        phoneStepsDataSource: PhoneStepsDataSource,
    ) : this(LocalDate::now, phoneStepsDataSource::readDailySteps, stepsRepository::replaceHealthConnectWindow)

    suspend fun sync() {
        val today = currentDate()
        val start = today.minusDays(RECENT_HISTORY_DAYS - 1)
        val totals = readDailySteps(start, today)
        replaceWindow(start, today, totals)
    }

    private companion object {
        const val RECENT_HISTORY_DAYS = 30L
    }
}
