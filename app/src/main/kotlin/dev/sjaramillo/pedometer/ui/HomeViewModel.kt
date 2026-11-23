package dev.sjaramillo.pedometer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import dev.sjaramillo.pedometer.data.StepsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    preferences: PedometerPreferences,
    private val stepsRepository: StepsRepository,
    private val coordinator: HealthConnectSyncCoordinator,
) : ViewModel() {
    private val date = MutableStateFlow(LocalDate.now())
    private var refreshJob: Job? = null

    val uiState =
        observeHomeUiState(date, stepsRepository::observeRecentDays, preferences.goal, coordinator.state, stepsRepository.observeAll())
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                HomeUiState(HomeTotals(date.value), goal = preferences.goal.value),
            )

    fun refresh() {
        val today = LocalDate.now()
        if (today == date.value && refreshJob?.isActive == true) return
        date.value = today
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { coordinator.refresh() }
    }
}

data class HomeUiState(
    val totals: HomeTotals,
    val goal: Int = DEFAULT_GOAL,
    val sync: HealthConnectSyncState = HealthConnectSyncState.Loading,
    val bestDay: DailySteps? = null,
    val lifetimeSteps: Long? = null,
) {
    val progress: Float
        get() = ((totals.today?.steps ?: 0L).toDouble() / goal).coerceIn(0.0, 1.0).toFloat()

    fun reachedGoal(steps: Long): Boolean = steps >= goal
}

data class HomeTotals(val date: LocalDate, val entries: List<DailySteps> = emptyList()) {
    val today: DailySteps?
        get() = entries.firstOrNull { it.day == date.toEpochDay() }

    internal fun currentWeek(firstDay: DayOfWeek): List<WeekDay> {
        val start = date.with(TemporalAdjusters.previousOrSame(firstDay))
        val byDay = entries.associateBy(DailySteps::day)
        return List(7) { index ->
            val day = start.plusDays(index.toLong())
            WeekDay(day, byDay[day.toEpochDay()]?.takeUnless { day.isAfter(date) })
        }
    }
}

internal data class WeekDay(val date: LocalDate, val total: DailySteps?)

internal data class WeekSummary(val total: Long, val average: Long)

internal fun summarizeWeek(
    days: List<WeekDay>,
    today: LocalDate,
): WeekSummary {
    val elapsed = days.filter { it.date <= today }
    val total = elapsed.sumOf { it.total?.steps ?: 0L }
    return WeekSummary(total, total / elapsed.size.coerceAtLeast(1))
}

internal fun observeHomeUiState(
    dates: Flow<LocalDate>,
    observeRecentDays: (LocalDate) -> Flow<List<DailySteps>>,
    goals: Flow<Int>,
    sync: Flow<HealthConnectSyncState>,
    history: Flow<List<DailySteps>>,
): Flow<HomeUiState> =
    combine(observeHomeTotals(dates, observeRecentDays), goals, sync, history) {
        totals,
        goal,
        syncState,
        entries,
        ->
        HomeUiState(
            totals = totals,
            goal = goal,
            sync = syncState,
            bestDay = entries.maxByOrNull(DailySteps::steps),
            lifetimeSteps = entries.sumOf(DailySteps::steps),
        )
    }

@OptIn(ExperimentalCoroutinesApi::class)
private fun observeHomeTotals(
    dates: Flow<LocalDate>,
    observeRecentDays: (LocalDate) -> Flow<List<DailySteps>>,
): Flow<HomeTotals> =
    dates.flatMapLatest { date ->
        observeRecentDays(date).map { entries -> HomeTotals(date, entries) }
    }
