package dev.sjaramillo.pedometer.data

import androidx.room.withTransaction
import java.time.LocalDate
import javax.inject.Inject

class StepsRepository @Inject constructor(
    private val database: PedometerDatabase,
) {
    private val dailyStepsDao = database.dailyStepsDao()

    suspend fun getAll(): List<DailySteps> = dailyStepsDao.getAll()

    suspend fun getStepsToday(): Long = dailyStepsDao.getSteps(LocalDate.now().toEpochDay()) ?: 0

    suspend fun getLastEntries(num: Int): List<DailySteps> = dailyStepsDao.getLastEntries(num)

    suspend fun importDailySteps(dailySteps: List<DailySteps>) {
        database.withTransaction {
            dailyStepsDao.insertAll(dailySteps)
        }
    }
}
