package dev.sjaramillo.nemi.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sjaramillo.nemi.R
import dev.sjaramillo.nemi.data.DailySteps
import dev.sjaramillo.nemi.data.HealthConnectSyncCoordinator
import dev.sjaramillo.nemi.data.HealthConnectSyncState
import dev.sjaramillo.nemi.data.StepHistoryStats
import dev.sjaramillo.nemi.data.StepsRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

@Inject
class HomeViewModel(
    preferences: NemiPreferences,
    private val stepsRepository: StepsRepository,
    private val coordinator: HealthConnectSyncCoordinator,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())
    private val weekSelection = MutableStateFlow(WeekSelection())
    private val firstDayOfWeek = MutableStateFlow(WeekFields.of(Locale.getDefault()).firstDayOfWeek)
    private var refreshJob: Job? = null

    internal val uiState =
        observeHomeUiState(
            date,
            weekSelection,
            firstDayOfWeek,
            preferences.goal,
            coordinator.state,
            stepsRepository::observeDay,
            stepsRepository::observeDays,
            stepsRepository.observeHistoryStats(),
        ).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HomeUiState(
                date = date.value,
                week = weekSelection.value.range(date.value, firstDayOfWeek.value).withEntries(emptyList()),
                goal = preferences.goal.value,
            ),
        )

    internal fun updateFirstDayOfWeek(firstDay: DayOfWeek) {
        firstDayOfWeek.value = firstDay
    }

    internal fun previousWeek() {
        weekSelection.update { it.previous(date.value, firstDayOfWeek.value) }
    }

    internal fun nextWeek() {
        weekSelection.update { it.next(date.value, firstDayOfWeek.value) }
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
    val date: LocalDate,
    val week: WeeklySteps,
    val today: DailySteps? = null,
    val goal: Int = DEFAULT_GOAL,
    val sync: HealthConnectSyncState = HealthConnectSyncState.Loading,
    val bestDay: DailySteps? = null,
    val lifetimeSteps: Long? = null,
) {
    val todayEmptyMessage: Int?
        get() =
            when {
                today != null -> null
                sync == HealthConnectSyncState.Ready -> R.string.no_steps_today
                else -> R.string.no_step_total_today
            }

    val progress: Float
        get() = ((today?.steps ?: 0L).toDouble() / goal).coerceIn(0.0, 1.0).toFloat()

    fun reachedGoal(steps: Long): Boolean = steps >= goal
}

@OptIn(ExperimentalCoroutinesApi::class)
internal fun observeHomeUiState(
    dates: Flow<LocalDate>,
    weekSelections: Flow<WeekSelection>,
    firstDays: Flow<DayOfWeek>,
    goals: Flow<Int>,
    sync: Flow<HealthConnectSyncState>,
    observeDay: (LocalDate) -> Flow<DailySteps?>,
    observeDays: (LocalDate, LocalDate) -> Flow<List<DailySteps>>,
    historyStats: Flow<StepHistoryStats>,
): Flow<HomeUiState> {
    val today = dates.flatMapLatest { date -> observeDay(date).map { date to it } }
    val weeks =
        combine(dates, weekSelections, firstDays) { date, selection, firstDay -> selection.range(date, firstDay) }
            .distinctUntilChanged()
            .flatMapLatest { range -> observeDays(range.start, range.through).map(range::withEntries) }

    return combine(today, weeks, goals, sync, historyStats) { (date, steps), week, goal, syncState, stats ->
        HomeUiState(
            date = date,
            week = week,
            today = steps,
            goal = goal,
            sync = syncState,
            bestDay = stats.bestDay,
            lifetimeSteps = stats.lifetimeSteps,
        )
    }
}
