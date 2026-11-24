package dev.sjaramillo.nemi.ui

import dev.sjaramillo.nemi.data.DailySteps
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

internal data class WeeklySteps(val days: List<WeekDay>, val range: WeekRange) {
    val isEmpty: Boolean
        get() = days.none { it.total != null }

    val total: Long
        get() = days.sumOf { it.total?.steps ?: 0L }

    val average: Long
        get() = total / days.count { it.date <= range.through }.coerceAtLeast(1)

    fun scaleMaximum(goal: Int): Long = maxOf(goal.toLong(), days.maxOf { it.total?.steps ?: 0L })
}

internal data class WeekRange(
    val start: LocalDate,
    val through: LocalDate,
    val canGoNext: Boolean,
) {
    fun withEntries(entries: List<DailySteps>): WeeklySteps {
        val byDay = entries.associateBy(DailySteps::day)
        return WeeklySteps(
            days =
                List(7) { index ->
                    val day = start.plusDays(index.toLong())
                    WeekDay(day, byDay[day.toEpochDay()]?.takeIf { day <= through })
                },
            range = this,
        )
    }
}

internal fun WeekSelection.range(
    today: LocalDate,
    firstDay: DayOfWeek,
): WeekRange {
    val start = start(today, firstDay)
    return WeekRange(start, minOf(start.plusDays(6), today), canGoNext(today, firstDay))
}
