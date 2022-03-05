package dev.sjaramillo.pedometer.ui.stats

data class StatsData(
    val recordSteps: String = "0",
    val recordDate: String = "",
    val totalStepsLast7Days: String = "0",
    val averageStepsLast7Days: String = "0",
    val totalStepsThisMonth: String = "0",
    val averageStepsThisMonth: String = "0",
    val totalStepsThisYear: String = "0",
    val averageStepsThisYear: String = "0",
    val totalStepsAllTime: String = "0",
    val averageStepsAllTime: String = "0",
)
