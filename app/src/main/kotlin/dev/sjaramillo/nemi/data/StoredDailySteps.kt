package dev.sjaramillo.nemi.data

data class StoredDailySteps(
    val day: Long,
    val steps: Long,
    val source: StepSource,
)

enum class StepSource {
    HEALTH_CONNECT,
    BACKUP,
}
