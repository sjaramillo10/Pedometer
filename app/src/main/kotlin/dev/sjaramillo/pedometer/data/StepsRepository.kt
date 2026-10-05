package dev.sjaramillo.pedometer.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

class StepsRepository @Inject constructor(
    private val database: PedometerDatabase,
) {
    private val dailyStepsDao = database.dailyStepsDao()

    fun observeAll(): Flow<List<DailySteps>> = dailyStepsDao.observeAll().distinctUntilChanged()

    suspend fun getAll(): List<DailySteps> = dailyStepsDao.getAll()

    suspend fun importDailySteps(dailySteps: List<DailySteps>) {
        database.withTransaction {
            dailyStepsDao.insertAll(dailySteps)
        }
    }
}
