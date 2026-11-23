package dev.sjaramillo.pedometer.ui

import dev.sjaramillo.pedometer.data.DailySteps
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

internal data class WeekSelection(private val anchor: LocalDate? = null) {
    fun start(
        today: LocalDate,
        firstDay: DayOfWeek,
    ): LocalDate = minOf(anchor ?: today, today).with(TemporalAdjusters.previousOrSame(firstDay))

    fun canGoNext(
        today: LocalDate,
        firstDay: DayOfWeek,
    ): Boolean = start(today, firstDay) < WeekSelection().start(today, firstDay)

    fun previous(
        today: LocalDate,
        firstDay: DayOfWeek,
    ): WeekSelection = WeekSelection(start(today, firstDay).minusWeeks(1))

    fun next(
        today: LocalDate,
        firstDay: DayOfWeek,
    ): WeekSelection {
        val next = start(today, firstDay).plusWeeks(1)
        return if (next < WeekSelection().start(today, firstDay)) WeekSelection(next) else WeekSelection()
    }
}

internal data class WeekDay(val date: LocalDate, val total: DailySteps?)

internal data class WeeklySteps(val days: List<WeekDay>, val today: LocalDate) {
    val isEmpty: Boolean
        get() = days.none { it.total != null }

    val total: Long
        get() = days.sumOf { it.total?.steps ?: 0L }

    val average: Long
        get() = total / days.count { it.date <= today }.coerceAtLeast(1)

    fun scaleMaximum(goal: Int): Long = maxOf(goal.toLong(), days.maxOf { it.total?.steps ?: 0L })
}

internal fun HomeTotals.week(
    selection: WeekSelection,
    firstDay: DayOfWeek,
): WeeklySteps {
    val start = selection.start(date, firstDay)
    val range = start.toEpochDay()..minOf(start.plusDays(6), date).toEpochDay()
    val byDay = entries.filter { it.day in range }.associateBy(DailySteps::day)
    return WeeklySteps(
        days =
            List(7) { index ->
                val day = start.plusDays(index.toLong())
                WeekDay(day, byDay[day.toEpochDay()])
            },
        today = date,
    )
}
