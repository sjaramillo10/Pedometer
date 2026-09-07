package dev.sjaramillo.pedometer.ui.home

import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import dev.sjaramillo.pedometer.data.StepsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    private lateinit var fakeStepsRepository: FakeStepsRepository
    private lateinit var fakeSyncCoordinator: FakeHealthConnectSyncCoordinator
    private lateinit var fakePreferences: FakeHomePreferences
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        fakeStepsRepository =
            FakeStepsRepository(
                stepsToday = 5000,
                lastEntries = listOf(DailySteps(2, 1000), DailySteps(1, 2000), DailySteps(0, 3000)),
            )
        fakeSyncCoordinator = FakeHealthConnectSyncCoordinator()
        fakePreferences = FakeHomePreferences(goal = 8000, stepSize = 70f, stepSizeCm = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load populates steps goal and step size from sources`() =
        runTest(dispatcher) {
            viewModel = HomeViewModel(fakeStepsRepository, fakeSyncCoordinator, fakePreferences)
            viewModel.load()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(5000L, state.stepsToday)
            assertEquals(listOf(2L, 1L, 0L), state.lastEntries.map(DailySteps::day))
            assertEquals(8000, state.goal)
            assertEquals(70f, state.stepSize)
            assertTrue(state.stepSizeCm)
        }

    @Test
    fun `toggleUnit flips showSteps`() =
        runTest(dispatcher) {
            viewModel = HomeViewModel(fakeStepsRepository, fakeSyncCoordinator, fakePreferences)
            assertTrue(viewModel.uiState.value.showSteps)

            viewModel.toggleUnit()
            assertFalse(viewModel.uiState.value.showSteps)

            viewModel.toggleUnit()
            assertTrue(viewModel.uiState.value.showSteps)
        }

    @Test
    fun `observeHealthConnectState reloads when sync becomes Ready`() =
        runTest(dispatcher) {
            viewModel = HomeViewModel(fakeStepsRepository, fakeSyncCoordinator, fakePreferences)
            viewModel.observeHealthConnectState()
            advanceUntilIdle()

            fakeStepsRepository.stepsToday = 9000
            fakeSyncCoordinator.setState(HealthConnectSyncState.Ready)
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(HealthConnectSyncState.Ready, state.healthConnectState)
            assertEquals(9000L, state.stepsToday)
        }
}

private class FakeStepsRepository(
    var stepsToday: Long,
    var lastEntries: List<DailySteps>,
) : StepsRepository {
    override suspend fun getAll(): List<DailySteps> = lastEntries

    override suspend fun getStepsToday(): Long = stepsToday

    override suspend fun getLastEntries(num: Int): List<DailySteps> = lastEntries

    override suspend fun importDailySteps(dailySteps: List<DailySteps>) = Unit
}

private class FakeHealthConnectSyncCoordinator : HealthConnectSyncCoordinator {
    private val _state = MutableStateFlow<HealthConnectSyncState>(HealthConnectSyncState.Loading)
    override val state: StateFlow<HealthConnectSyncState> = _state

    override suspend fun refresh() = Unit

    fun setState(newState: HealthConnectSyncState) {
        _state.value = newState
    }
}

private class FakeHomePreferences(
    override val goal: Int,
    override val stepSize: Float,
    override val stepSizeCm: Boolean,
) : HomePreferences
