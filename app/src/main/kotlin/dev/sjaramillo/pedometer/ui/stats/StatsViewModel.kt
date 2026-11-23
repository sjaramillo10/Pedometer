package dev.sjaramillo.pedometer.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.util.DateUtil
import dev.sjaramillo.pedometer.util.FormatUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val stepsRepository: StepsRepository,
) : ViewModel() {
    private val numberFormat = FormatUtil.numberFormat
    private val dateFormat = FormatUtil.dateFormat

    private val _uiState = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val uiState: StateFlow<StatsUiState> = _uiState

    init {
        viewModelScope.launch {
            loadStats()
        }
    }

    private suspend fun loadStats() {
        _uiState.value = StatsUiState.Success(createStatsData(stepsRepository.getAll()))
    }

    private fun createStatsData(dailySteps: List<DailySteps>): StatsData {
        val today = LocalDate.now()
        val stepsByDay = dailySteps.associate { DateUtil.dayToLocalDate(it.day) to it.steps }

        fun totalSince(start: LocalDate): Long =
            generateSequence(start) { day -> day.plusDays(1).takeIf { it <= today } }
                .sumOf { stepsByDay[it] ?: 0 }

        val record = dailySteps.maxByOrNull(DailySteps::steps)
        val totalLast7Days = totalSince(today.minusDays(6))
        val totalThisMonth = totalSince(today.withDayOfMonth(1))
        val totalThisYear = totalSince(today.withDayOfYear(1))
        val totalAllTime = dailySteps.sumOf(DailySteps::steps)
        val totalDays = dailySteps.size.coerceAtLeast(1)

        return StatsData(
            recordSteps = numberFormat.format(record?.steps ?: 0),
            recordDate = record?.let { dateFormat.format(DateUtil.dayToLocalDate(it.day)) } ?: "—",
            totalStepsLast7Days = numberFormat.format(totalLast7Days),
            averageStepsLast7Days = numberFormat.format(totalLast7Days / 7),
            totalStepsThisMonth = numberFormat.format(totalThisMonth),
            averageStepsThisMonth = numberFormat.format(totalThisMonth / today.dayOfMonth),
            totalStepsThisYear = numberFormat.format(totalThisYear),
            averageStepsThisYear = numberFormat.format(totalThisYear / today.dayOfYear),
            totalStepsAllTime = numberFormat.format(totalAllTime),
            averageStepsAllTime = numberFormat.format(totalAllTime / totalDays),
        )
    }
}

sealed class StatsUiState {
    data object Loading : StatsUiState()

    data class Success(val statsData: StatsData) : StatsUiState()
}
