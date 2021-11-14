package dev.sjaramillo.pedometer.ui.stats

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.util.DateUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val stepsRepository: StepsRepository
) : ViewModel() {

    fun getStatsData(): Flow<StatsData> {
        val today = DateUtil.getToday()
        val dayOfMonth = DateUtil.getDayOfMonth()

        return combine(
            stepsRepository.getRecord(),
            stepsRepository.getStepsFromDayRangeFlow(today - 6, today),
            stepsRepository.getStepsFromDayRangeFlow(today - dayOfMonth + 1, today)
        ) { record, totalLast7Days, totalThisMonth ->
            StatsData(
                record = record,
                totalLast7Days = totalLast7Days,
                averageLast7Days = totalLast7Days / 7,
                totalThisMonth = totalThisMonth,
                averageThisMonth = totalThisMonth / dayOfMonth,
            )
        }
    }
}
