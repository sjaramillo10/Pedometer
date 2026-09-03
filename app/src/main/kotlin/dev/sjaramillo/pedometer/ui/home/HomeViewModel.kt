package dev.sjaramillo.pedometer.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.ui.SettingsFragment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stepsRepository: StepsRepository,
    private val healthConnectSyncCoordinator: HealthConnectSyncCoordinator,
    @ApplicationContext context: Context,
) : ViewModel() {
    data class HomeUiState(
        val goal: Int = SettingsFragment.DEFAULT_GOAL,
        val showSteps: Boolean = true,
        val stepsToday: Long = 0,
        val lastEntries: List<DailySteps> = emptyList(),
        val stepSize: Float = SettingsFragment.DEFAULT_STEP_SIZE,
        val stepSizeCm: Boolean = SettingsFragment.DEFAULT_STEP_UNIT == "cm",
        val healthConnectState: HealthConnectSyncState = HealthConnectSyncState.Loading,
    )

    private val preferences =
        context.getSharedPreferences("pedometer", Context.MODE_PRIVATE)

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

    private suspend fun refresh() {
        val goal = preferences.getInt("goal", SettingsFragment.DEFAULT_GOAL)
        val stepSize = preferences.getFloat("step_size_value", SettingsFragment.DEFAULT_STEP_SIZE)
        val stepSizeCm =
            preferences.getString("step_size_unit", SettingsFragment.DEFAULT_STEP_UNIT) == "cm"
        val stepsToday = stepsRepository.getStepsToday()
        val lastEntries = stepsRepository.getLastEntries(8)

        _uiState.update {
            it.copy(
                goal = goal,
                stepSize = stepSize,
                stepSizeCm = stepSizeCm,
                stepsToday = stepsToday,
                lastEntries = lastEntries,
            )
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

    private fun MutableStateFlow<HomeUiState>.update(transform: (HomeUiState) -> HomeUiState) {
        this.value = transform(this.value)
    }
}
