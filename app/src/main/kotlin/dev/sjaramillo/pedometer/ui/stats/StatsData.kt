package dev.sjaramillo.pedometer.ui.stats

import dev.sjaramillo.pedometer.data.DailySteps

data class StatsData(
    val record: DailySteps = DailySteps(day = 0, steps = 0),
    val totalLast7Days: Long = 0,
    val averageLast7Days: Long = 0,
    val totalThisMonth: Long = 0,
    val averageThisMonth: Long = 0,
)
