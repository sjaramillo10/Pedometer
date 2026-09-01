package dev.sjaramillo.pedometer.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class StepsRepository @Inject constructor(db: PedometerDatabase) {
    private val dailyStepsDao = db.dailyStepsDao()

    fun getAllFlow(): Flow<List<DailySteps>> = dailyStepsDao.getAllFlow()

    fun getStepsTodayFlow(): Flow<Long> =
        dailyStepsDao.getStepsFlow(LocalDate.now().toEpochDay()).map { it ?: 0 }

    fun getLastEntriesFlow(num: Int): Flow<List<DailySteps>> = dailyStepsDao.getLastEntriesFlow(num)

    suspend fun getStepsFromDayRange(
        start: Long,
        end: Long,
    ): Long = dailyStepsDao.getStepsFromDayRange(start, end) ?: 0L
}
