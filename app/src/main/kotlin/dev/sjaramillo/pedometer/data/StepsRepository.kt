package dev.sjaramillo.pedometer.data

import java.time.LocalDate
import javax.inject.Inject

class StepsRepository @Inject constructor(db: PedometerDatabase) {
    private val dailyStepsDao = db.dailyStepsDao()

    suspend fun getAll(): List<DailySteps> = dailyStepsDao.getAll()

    suspend fun getStepsToday(): Long = dailyStepsDao.getSteps(LocalDate.now().toEpochDay()) ?: 0

    suspend fun getLastEntries(num: Int): List<DailySteps> = dailyStepsDao.getLastEntries(num)
}
