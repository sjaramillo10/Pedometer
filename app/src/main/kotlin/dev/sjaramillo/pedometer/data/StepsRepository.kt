package dev.sjaramillo.pedometer.data

interface StepsRepository {
    suspend fun getAll(): List<DailySteps>

    suspend fun getStepsToday(): Long

    suspend fun getLastEntries(num: Int): List<DailySteps>

    suspend fun importDailySteps(dailySteps: List<DailySteps>)
}
