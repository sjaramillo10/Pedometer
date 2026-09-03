package dev.sjaramillo.pedometer.data

import java.time.LocalDate

/** Preserves imported history when Health Connect reports no steps for a recent day. */
internal fun mergeRecentSteps(
    start: LocalDate,
    end: LocalDate,
    healthConnectStepsByDay: Map<LocalDate, Long>,
    cachedSteps: List<DailySteps>,
): List<DailySteps> {
    val cachedStepsByDay = cachedSteps.associate { it.day to it.steps }

    return (
        generateSequence(start) { day -> day.plusDays(1).takeIf { it <= end } }
            .map { day ->
                val healthConnectSteps = healthConnectStepsByDay[day] ?: 0
                val cachedStepsForDay = cachedStepsByDay[day.toEpochDay()] ?: 0
                val steps =
                    if (healthConnectSteps == 0L) {
                        maxOf(cachedStepsForDay, 0)
                    } else {
                        healthConnectSteps
                    }
                DailySteps(day.toEpochDay(), steps)
            }.toList()
    )
}
