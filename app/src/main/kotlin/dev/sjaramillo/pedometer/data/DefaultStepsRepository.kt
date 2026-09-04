package dev.sjaramillo.pedometer.data

import androidx.room.withTransaction
import java.time.LocalDate
import javax.inject.Inject

class DefaultStepsRepository @Inject constructor(
    private val database: PedometerDatabase,
) : StepsRepository {
    private val dailyStepsDao = database.dailyStepsDao()

    override suspend fun getAll(): List<DailySteps> = dailyStepsDao.getAll()

    override suspend fun getStepsToday(): Long = dailyStepsDao.getSteps(LocalDate.now().toEpochDay()) ?: 0

    override suspend fun getLastEntries(num: Int): List<DailySteps> = dailyStepsDao.getLastEntries(num)

    override suspend fun importDailySteps(dailySteps: List<DailySteps>) {
        database.withTransaction {
            dailyStepsDao.insertAll(dailySteps)
        }
    }
}
