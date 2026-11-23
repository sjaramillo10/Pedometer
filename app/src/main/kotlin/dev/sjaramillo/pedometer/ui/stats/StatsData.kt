package dev.sjaramillo.pedometer.ui.stats

import dev.sjaramillo.pedometer.data.DailySteps

data class StatsData(
    val bestDay: DailySteps?,
    val lifetimeSteps: Long,
)
