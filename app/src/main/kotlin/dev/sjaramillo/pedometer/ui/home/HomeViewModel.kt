package dev.sjaramillo.pedometer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import dev.sjaramillo.pedometer.data.StepsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stepsRepository: StepsRepository,
    private val healthConnectSyncCoordinator: HealthConnectSyncCoordinator,
    private val preferences: HomePreferences,
) : ViewModel() {
    data class HomeUiState(
        val goal: Int = 10000,
        val showSteps: Boolean = true,
        val stepsToday: Long = 0,
        val lastEntries: List<DailySteps> = emptyList(),
        val stepSize: Float = 75f,
        val stepSizeCm: Boolean = true,
        val healthConnectState: HealthConnectSyncState = HealthConnectSyncState.Loading,
    )

    private val stepSize: Float
        get() = preferences.stepSize

    private val stepSizeCm: Boolean
        get() = preferences.stepSizeCm

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    fun toggleUnit() {
        _uiState.update { it.copy(showSteps = !it.showSteps) }
    }

    fun load() {
        viewModelScope.launch {
            refresh()
        }
    }

    fun observeHealthConnectState() {
        viewModelScope.launch {
            healthConnectSyncCoordinator.state.collect { state ->
                _uiState.update { it.copy(healthConnectState = state) }
                if (state == HealthConnectSyncState.Ready) {
                    refresh()
                }
            }
        }
    }

    private suspend fun refresh() {
        val stepsToday = stepsRepository.getStepsToday()
        val lastEntries = stepsRepository.getLastEntries(8)

        _uiState.update {
            it.copy(
                goal = preferences.goal,
                stepSize = stepSize,
                stepSizeCm = stepSizeCm,
                stepsToday = stepsToday,
                lastEntries = lastEntries,
            )
        }
    }

    private fun MutableStateFlow<HomeUiState>.update(transform: (HomeUiState) -> HomeUiState) {
        this.value = transform(this.value)
    }
}
