package dev.sjaramillo.pedometer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import dev.sjaramillo.pedometer.data.StepsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    preferences: PedometerPreferences,
    private val stepsRepository: StepsRepository,
    private val coordinator: HealthConnectSyncCoordinator,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())
    private val weekSelection = MutableStateFlow(WeekSelection())
    private var refreshJob: Job? = null

    internal val uiState =
        observeHomeUiState(date, weekSelection, preferences.goal, coordinator.state, stepsRepository.observeAll())
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                HomeUiState(HomeTotals(date.value), goal = preferences.goal.value),
            )

    internal fun previousWeek(firstDay: DayOfWeek) {
        weekSelection.update { it.previous(date.value, firstDay) }
    }

    internal fun nextWeek(firstDay: DayOfWeek) {
        weekSelection.update { it.next(date.value, firstDay) }
    }

    fun refresh(restart: Boolean = false) {
        val today = LocalDate.now()
        if (!restart && today == date.value && refreshJob?.isActive == true) return
        date.value = today
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { coordinator.refresh() }
    }
}

internal data class HomeUiState(
    val totals: HomeTotals,
    val goal: Int = DEFAULT_GOAL,
    val sync: HealthConnectSyncState = HealthConnectSyncState.Loading,
    val bestDay: DailySteps? = null,
    val lifetimeSteps: Long? = null,
    val weekSelection: WeekSelection = WeekSelection(),
) {
    val progress: Float
        get() = ((totals.today?.steps ?: 0L).toDouble() / goal).coerceIn(0.0, 1.0).toFloat()

    fun reachedGoal(steps: Long): Boolean = steps >= goal
}

internal data class HomeTotals(val date: LocalDate, val entries: List<DailySteps> = emptyList()) {
    val today: DailySteps?
        get() = entries.firstOrNull { it.day == date.toEpochDay() }
}

internal fun observeHomeUiState(
    dates: Flow<LocalDate>,
    weekSelections: Flow<WeekSelection>,
    goals: Flow<Int>,
    sync: Flow<HealthConnectSyncState>,
    history: Flow<List<DailySteps>>,
): Flow<HomeUiState> =
    combine(
        dates,
        weekSelections,
        goals,
        sync,
        history.map { entries -> Triple(entries, entries.maxByOrNull(DailySteps::steps), entries.sumOf(DailySteps::steps)) },
    ) {
        date,
        selection,
        goal,
        syncState,
        (entries, bestDay, lifetimeSteps),
        ->
        HomeUiState(
            totals = HomeTotals(date, entries),
            weekSelection = selection,
            goal = goal,
            sync = syncState,
            bestDay = bestDay,
            lifetimeSteps = lifetimeSteps,
        )
    }
