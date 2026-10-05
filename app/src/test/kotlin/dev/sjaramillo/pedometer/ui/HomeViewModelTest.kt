package dev.sjaramillo.pedometer.ui

import androidx.lifecycle.viewModelScope
import dev.sjaramillo.pedometer.data.DailySteps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    @Test
    fun `home retains updated history while the view has no collector`() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val rows = MutableSharedFlow<List<DailySteps>>()
            val model = HomeViewModel(rows)
            try {
                advanceUntilIdle()
                val initial = listOf(DailySteps(20000, 1500))
                rows.emit(initial)
                advanceUntilIdle()
                assertEquals(initial, model.entries.first())

                val updated = listOf(DailySteps(20000, 4500), DailySteps(20001, 14000))
                rows.emit(updated)
                advanceUntilIdle()

                assertEquals(updated, model.entries.value)
                assertEquals(updated, model.entries.first())
                assertEquals(1, rows.subscriptionCount.value)
            } finally {
                model.viewModelScope.cancel()
                Dispatchers.resetMain()
            }
        }
}
