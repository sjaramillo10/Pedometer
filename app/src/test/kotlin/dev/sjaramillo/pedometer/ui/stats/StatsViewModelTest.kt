package dev.sjaramillo.pedometer.ui.stats

import androidx.lifecycle.viewModelScope
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.util.FormatUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {
    private val today = LocalDate.of(2026, 10, 4)
    private val clock = Clock.fixed(Instant.parse("2026-10-04T12:00:00Z"), ZoneOffset.UTC)

    @Test
    fun `stats refresh when synchronized or imported rows change`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val entries = MutableStateFlow(listOf(DailySteps(today.toEpochDay(), 1500)))
            val model = StatsViewModel(entries, clock)
            try {
                advanceUntilIdle()
                assertEquals(formatted(1500), stats(model).totalStepsAllTime)

                entries.value = listOf(DailySteps(today.minusDays(1).toEpochDay(), 4500), DailySteps(today.toEpochDay(), 14000))
                advanceUntilIdle()

                assertEquals(formatted(18500), stats(model).totalStepsLast7Days)
                assertEquals(formatted(14000), stats(model).recordSteps)
                assertEquals(formatted(18500), stats(model).totalStepsThisMonth)

                entries.value = listOf(DailySteps(today.minusYears(1).toEpochDay(), 20000), DailySteps(today.toEpochDay(), 7500))
                advanceUntilIdle()

                assertEquals(formatted(27500), stats(model).totalStepsAllTime)
                assertEquals(formatted(7500), stats(model).totalStepsThisYear)
            } finally {
                model.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun `empty history updates after initial sync`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val entries = MutableStateFlow(emptyList<DailySteps>())
            val model = StatsViewModel(entries, clock)
            try {
                advanceUntilIdle()
                assertEquals(formatted(0), stats(model).totalStepsAllTime)
                assertEquals("—", stats(model).recordDate)

                entries.value = listOf(DailySteps(today.toEpochDay(), 10500))
                advanceUntilIdle()

                assertEquals(formatted(10500), stats(model).totalStepsAllTime)
            } finally {
                model.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }

    private fun stats(model: StatsViewModel): StatsData = (model.uiState.value as StatsUiState.Success).statsData

    private fun formatted(value: Long): String = FormatUtil.numberFormat.format(value)
}
