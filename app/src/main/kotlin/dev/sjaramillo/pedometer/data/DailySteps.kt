package dev.sjaramillo.pedometer.data

import java.time.LocalDate

data class DailySteps(
    val day: Long,
    val steps: Long,
    val fromBackup: Boolean,
)

internal val lastSupportedEpochDay = LocalDate.MAX.toEpochDay()

internal fun isSupportedStepDay(day: Long): Boolean = day in 1..lastSupportedEpochDay
