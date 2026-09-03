package dev.sjaramillo.pedometer.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class RecentStepsMergeTest {
    private val start = LocalDate.of(2026, 9, 1)
    private val end = LocalDate.of(2026, 9, 3)

    @Test
    fun `uses Health Connect values when they are positive`() {
        val merged =
            mergeRecentSteps(
                start = start,
                end = end,
                healthConnectStepsByDay = mapOf(start.plusDays(1) to 500),
                cachedSteps = listOf(DailySteps(start.plusDays(1).toEpochDay(), 900)),
            )

        assertEquals(
            listOf(
                DailySteps(start.toEpochDay(), 0),
                DailySteps(start.plusDays(1).toEpochDay(), 500),
                DailySteps(end.toEpochDay(), 0),
            ),
            merged,
        )
    }

    @Test
    fun `preserves a higher cached value when Health Connect reports zero`() {
        val merged =
            mergeRecentSteps(
                start = start,
                end = end,
                healthConnectStepsByDay = mapOf(start.plusDays(1) to 0),
                cachedSteps = listOf(DailySteps(start.plusDays(1).toEpochDay(), 900)),
            )

        assertEquals(900L, merged[1].steps)
    }
}
