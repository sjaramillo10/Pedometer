package dev.sjaramillo.pedometer.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.StepsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    stepsRepository: StepsRepository,
) : ViewModel() {
    val uiState =
        observeStats(stepsRepository.observeAll())
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

internal fun observeStats(totals: Flow<List<DailySteps>>): Flow<StatsData> =
    totals
        .map { steps ->
            StatsData(
                bestDay = steps.maxByOrNull(DailySteps::steps),
                lifetimeSteps = steps.sumOf(DailySteps::steps),
            )
        }.distinctUntilChanged()
