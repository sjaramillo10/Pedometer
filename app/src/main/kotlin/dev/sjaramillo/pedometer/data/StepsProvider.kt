package dev.sjaramillo.pedometer.data

import java.time.LocalDate

interface StepsProvider {
    suspend fun access(): StepsAccess

    suspend fun readDailySteps(
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Long>
}

enum class StepsAccess {
    Available,
    PermissionRequired,
    Unavailable,
}

interface StepCacheSynchronizer {
    suspend fun sync()
}
