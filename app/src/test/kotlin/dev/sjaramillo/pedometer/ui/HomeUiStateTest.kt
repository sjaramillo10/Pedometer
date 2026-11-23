package dev.sjaramillo.pedometer.ui

import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.HealthConnectAccess
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class HomeUiStateTest {
    private val today = LocalDate.of(2026, 11, 22)
    private val previous = DailySteps(today.minusDays(1).toEpochDay(), 5_000, fromBackup = true)
    private val current = DailySteps(today.toEpochDay(), 10_000, fromBackup = false)

    @Test
    fun `given active Home state when the shared goal changes then Home emits the new goal`() =
        runBlocking {
            // Given
            val goals = MutableStateFlow(12_000)
            val states =
                observeHomeUiState(
                    flowOf(today),
                    flowOf(WeekSelection()),
                    goals,
                    flowOf(HealthConnectSyncState.Ready),
                    flowOf(listOf(current)),
                )
            val updates = Channel<HomeUiState>(Channel.UNLIMITED)
            val observer = launch { states.collect { updates.send(it) } }

            // When
            try {
                withTimeout(5_000) {
                    val initial = updates.receive()
                    goals.value = 12_100
                    val updated = updates.receive()

                    // Then
                    assertEquals(12_000, initial.goal)
                    assertEquals(initial.copy(goal = 12_100), updated)
                }
            } finally {
                observer.cancel()
            }
        }

    @Test
    fun `given an open screen when backup and refresh commit then totals update and survive a failed refresh`() =
        runBlocking {
            // Given
            val rows = MutableStateFlow<List<DailySteps>>(emptyList())
            val older = DailySteps(today.minusDays(40).toEpochDay(), 1_000, fromBackup = true)
            val history = rows.map { listOf(older) + it }
            var refreshFails = false
            val coordinator =
                HealthConnectSyncCoordinator({ HealthConnectAccess.Available }) {
                    check(!refreshFails)
                    rows.value = listOf(previous, current)
                }
            val states = Channel<HomeUiState>(Channel.UNLIMITED)
            val observer =
                launch {
                    observeHomeUiState(flowOf(today), flowOf(WeekSelection()), flowOf(10_000), coordinator.state, history)
                        .collect { states.send(it) }
                }

            // When
            try {
                withTimeout(5_000) {
                    assertEquals(HomeTotals(today, listOf(older)), states.receive().totals)
                    rows.value = listOf(previous)
                    var imported = states.receive()
                    while (imported.bestDay != previous || imported.totals.entries != listOf(older, previous)) {
                        imported = states.receive()
                    }
                    coordinator.refresh()
                    var refreshed = states.receive()
                    while (refreshed.sync != HealthConnectSyncState.Ready ||
                        refreshed.totals.today != current ||
                        refreshed.lifetimeSteps != 16_000L
                    ) {
                        refreshed = states.receive()
                    }
                    refreshFails = true
                    coordinator.refresh()
                    var failed = states.receive()
                    while (failed.sync != HealthConnectSyncState.Error) {
                        failed = states.receive()
                    }

                    // Then
                    assertEquals(listOf(older, previous), imported.totals.entries)
                    assertNull(imported.totals.today)
                    assertEquals(6_000L, imported.lifetimeSteps)
                    assertEquals(current, refreshed.bestDay)
                    assertEquals(refreshed.bestDay, failed.bestDay)
                    assertEquals(refreshed.lifetimeSteps, failed.lifetimeSteps)
                    assertEquals(HomeTotals(today, listOf(older, previous, current)), refreshed.totals)
                    assertEquals(refreshed.totals, failed.totals)
                }
            } finally {
                observer.cancel()
            }
        }

    @Test
    fun `given observed totals when the local date rolls over then today becomes the newest history entry`() =
        runBlocking {
            // Given
            val dates = MutableStateFlow(today)
            val states = Channel<HomeUiState>(Channel.UNLIMITED)
            val observer =
                launch {
                    observeHomeUiState(
                        dates,
                        flowOf(WeekSelection()),
                        flowOf(10_000),
                        flowOf(HealthConnectSyncState.Ready),
                        flowOf(listOf(previous, current)),
                    ).collect { states.send(it) }
                }

            // When
            try {
                withTimeout(5_000) {
                    val before = states.receive()
                    dates.value = today.plusDays(1)
                    var after = states.receive()
                    while (after.totals.date == today) {
                        after = states.receive()
                    }

                    // Then
                    assertEquals(current, before.totals.today)
                    assertEquals(HomeTotals(today.plusDays(1), listOf(previous, current)), after.totals)
                }
            } finally {
                observer.cancel()
            }
        }

    @Test
    fun `given daily totals when building the locale week then bars distinguish recorded zero missing and upcoming days`() {
        // Given
        val monday = today.plusDays(1)
        val wednesday = monday.plusDays(2)
        val zero = DailySteps(monday.toEpochDay(), 0, fromBackup = true)
        val recorded = DailySteps(wednesday.toEpochDay(), 12_000, fromBackup = false)
        val future = DailySteps(wednesday.plusDays(1).toEpochDay(), 5000, fromBackup = true)
        val totals = HomeTotals(wednesday, listOf(current, zero, recorded, future))

        // When
        val mondayWeek = totals.week(WeekSelection(), DayOfWeek.MONDAY).days
        val sundayWeek = totals.week(WeekSelection(), DayOfWeek.SUNDAY).days

        // Then
        assertEquals((0L..6L).map(monday::plusDays), mondayWeek.map(WeekDay::date))
        assertEquals(listOf(zero, null, recorded, null, null, null, null), mondayWeek.map(WeekDay::total))
        assertEquals(WeekDay(today, current), sundayWeek.first())
    }

    @Test
    fun `given sparse history when summarizing current and past weeks then averages use the calendar days`() {
        // Given
        val monday = today.plusDays(1)
        val wednesday = monday.plusDays(2)
        val backup = DailySteps(monday.toEpochDay(), 10_001, fromBackup = true)
        val zero = DailySteps(wednesday.toEpochDay(), 0, fromBackup = false)
        val future = DailySteps(wednesday.plusDays(1).toEpochDay(), 20_000, fromBackup = false)
        val entries = listOf(backup, zero, future)
        val selection = WeekSelection()

        // When
        val firstDay = HomeTotals(monday, entries).week(selection, DayOfWeek.MONDAY)
        val midweek = HomeTotals(wednesday, entries).week(selection, DayOfWeek.MONDAY)
        val nextMonday = monday.plusWeeks(1)
        val past = HomeTotals(nextMonday, entries).week(selection.previous(nextMonday, DayOfWeek.MONDAY), DayOfWeek.MONDAY)

        // Then
        assertEquals(10_001L, firstDay.average)
        assertEquals(10_001L, midweek.total)
        assertEquals(3_333L, midweek.average)
        assertEquals(backup, midweek.days.first().total)
        assertEquals(30_001L, past.total)
        assertEquals(4_285L, past.average)
        assertEquals(12_000L, midweek.scaleMaximum(12_000))
        assertEquals(20_000L, past.scaleMaximum(12_000))
    }

    @Test
    fun `given the current week when browsing across New Year then navigation stops on return to current`() {
        // Given
        val january = LocalDate.of(2026, 1, 4)
        val monday = DayOfWeek.MONDAY
        val sunday = DayOfWeek.SUNDAY
        val current = WeekSelection()

        // When
        val past = current.previous(january, monday)
        val earlier = past.previous(january, monday)
        val returned = earlier.next(january, monday).next(january, monday)
        val rollover = january.plusDays(1)

        // Then
        assertEquals(LocalDate.of(2025, 12, 29), current.start(january, monday))
        assertEquals(january, current.start(january, sunday))
        assertEquals(LocalDate.of(2025, 12, 15), earlier.start(january, monday))
        assertTrue(earlier.canGoNext(january, monday))
        assertEquals(current, returned)
        assertFalse(returned.canGoNext(january, monday))
        assertEquals(current, returned.next(january, monday))
        assertEquals(rollover, returned.start(rollover, monday))
        assertEquals(past.start(january, monday), past.start(rollover, monday))
    }

    @Test
    fun `given missing history and a recorded zero when selecting their weeks then only missing history is empty`() {
        // Given
        val zero = DailySteps(today.toEpochDay(), 0, fromBackup = false)
        val totals = HomeTotals(today, listOf(zero))
        val current = WeekSelection()

        // When
        val recordedWeek = totals.week(current, DayOfWeek.SUNDAY)
        val emptyWeek = totals.week(current.previous(today, DayOfWeek.SUNDAY), DayOfWeek.SUNDAY)

        // Then
        assertFalse(recordedWeek.isEmpty)
        assertEquals(zero, recordedWeek.days.first().total)
        assertNull(recordedWeek.days[1].total)
        assertTrue(emptyWeek.isEmpty)
        assertEquals(0L, emptyWeek.total)
        assertEquals(0L, emptyWeek.average)
    }

    @Test
    fun `given a saved goal when comparing totals then equality reaches the goal`() {
        // Given
        val state = HomeUiState(totals = HomeTotals(today, listOf(current)), goal = 10_000)

        // When
        val atGoal = state.reachedGoal(current.steps)
        val belowGoal = state.copy(goal = 10_001).reachedGoal(current.steps)

        // Then
        assertTrue(atGoal)
        assertFalse(belowGoal)
        assertEquals(1f, state.progress, 0f)
        assertEquals(1f, state.copy(goal = 1).progress, 0f)
    }

    @Test
    fun `given a goal off the hundred grid when adjusted in either direction then it preserves its offset`() {
        // Given
        val goal = 7_550

        // When
        val adjusted = listOf(-100, 100).map { adjustGoal(goal, it) }

        // Then
        assertEquals(listOf(7_450, 7_650), adjusted)
    }

    @Test
    fun `given goals near or at the limits when adjusted outward then they clamp to the limits`() {
        // Given
        val lowerGoals = listOf(1, 50)
        val upperGoals = listOf(99_950, 100_000)

        // When
        val lowerResults = lowerGoals.map { adjustGoal(it, -100) }
        val upperResults = upperGoals.map { adjustGoal(it, 100) }

        // Then
        assertEquals(listOf(1, 1), lowerResults)
        assertEquals(listOf(100_000, 100_000), upperResults)
    }

    @Test
    fun `given a held goal button when successive repeats occur then the delay decreases to a minimum`() {
        // Given
        val initialDelay = FIRST_GOAL_REPEAT_DELAY

        // When
        val delays = generateSequence(initialDelay, ::nextGoalRepeatDelay).take(10).toList()

        // Then
        assertEquals(listOf(150L, 135L, 120L, 105L, 90L, 75L, 60L, 45L, 45L, 45L), delays)
    }
}
