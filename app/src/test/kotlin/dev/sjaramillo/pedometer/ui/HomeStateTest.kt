package dev.sjaramillo.pedometer.ui

import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.HealthConnectAccess
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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

class HomeStateTest {
    private val today = LocalDate.of(2026, 11, 22)
    private val previous = DailySteps(today.minusDays(1).toEpochDay(), 5_000, fromBackup = true)
    private val current = DailySteps(today.toEpochDay(), 10_000, fromBackup = false)

    @Test
    fun `given an open screen when backup and refresh commit then totals update and survive a failed refresh`() =
        runBlocking {
            // Given
            val rows = MutableStateFlow<List<DailySteps>>(emptyList())
            var refreshFails = false
            val coordinator =
                HealthConnectSyncCoordinator({ HealthConnectAccess.Available }) {
                    check(!refreshFails)
                    rows.value = listOf(previous, current)
                }
            val states = Channel<HomeState>(Channel.UNLIMITED)
            val observer =
                launch {
                    observeHomeState(flowOf(today), { rows }, flowOf(10_000), coordinator.state).collect { states.send(it) }
                }

            // When
            try {
                withTimeout(5_000) {
                    assertEquals(HomeTotals(today), states.receive().totals)
                    rows.value = listOf(previous)
                    val imported = states.receive()
                    coordinator.refresh()
                    var refreshed = states.receive()
                    while (refreshed.sync != HealthConnectSyncState.Ready || refreshed.totals.today != current) {
                        refreshed = states.receive()
                    }
                    refreshFails = true
                    coordinator.refresh()
                    var failed = states.receive()
                    while (failed.sync != HealthConnectSyncState.Error) {
                        failed = states.receive()
                    }

                    // Then
                    assertEquals(listOf(previous), imported.totals.entries)
                    assertNull(imported.totals.today)
                    assertEquals(HomeTotals(today, listOf(previous, current)), refreshed.totals)
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
            val states = Channel<HomeState>(Channel.UNLIMITED)
            val observer =
                launch {
                    observeHomeState(
                        dates,
                        { flowOf(listOf(previous, current)) },
                        flowOf(10_000),
                        flowOf(HealthConnectSyncState.Ready),
                    ).collect { states.send(it) }
                }

            // When
            try {
                withTimeout(5_000) {
                    val before = states.receive()
                    dates.value = today.plusDays(1)
                    val after = states.receive()

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
        val mondayWeek = totals.currentWeek(DayOfWeek.MONDAY)
        val sundayWeek = totals.currentWeek(DayOfWeek.SUNDAY)

        // Then
        assertEquals((0L..6L).map(monday::plusDays), mondayWeek.map(WeekDay::date))
        assertEquals(listOf(zero, null, recorded, null, null, null, null), mondayWeek.map(WeekDay::total))
        assertEquals(WeekDay(today, current), sundayWeek.first())
    }

    @Test
    fun `given an editable goal when parsing and comparing totals then only valid targets count and equality reaches the goal`() {
        // Given
        val inputs = listOf("", "0", "100001", "1.5", " 10000 ", "1", "100000")
        val state = HomeState(totals = HomeTotals(today, listOf(current)), goal = 10_000)

        // When
        val parsed = inputs.map(::parseGoal)
        val atGoal = state.reachedGoal(current.steps)
        val belowGoal = state.copy(goal = 10_001).reachedGoal(current.steps)

        // Then
        assertEquals(listOf(null, null, null, null, 10_000, 1, 100_000), parsed)
        assertTrue(atGoal)
        assertFalse(belowGoal)
        assertEquals(1f, state.progress, 0f)
        assertEquals(1f, state.copy(goal = 1).progress, 0f)
    }
}
