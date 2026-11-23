package dev.sjaramillo.pedometer.data

import androidx.room.Entity

@Entity(tableName = "daily_steps", primaryKeys = ["day", "source"])
data class StoredDailySteps(
    val day: Long,
    val steps: Long,
    val source: StepSource,
)

enum class StepSource {
    HEALTH_CONNECT,
    BACKUP,
}
