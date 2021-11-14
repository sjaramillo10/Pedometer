package dev.sjaramillo.pedometer.ui.stats

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.util.DateUtil
import dev.sjaramillo.pedometer.util.FormatUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val stepsRepository: StepsRepository
) : ViewModel() {

    private val numberFormat = FormatUtil.numberFormat
    private val dateFormat = FormatUtil.dateFormat

    fun getStatsData(): Flow<StatsData> {
        val today = DateUtil.getToday()
        val dayOfMonth = DateUtil.getDayOfMonth()

        return combine(
            stepsRepository.getRecord(),
            stepsRepository.getStepsFromDayRangeFlow(today - 6, today),
            stepsRepository.getStepsFromDayRangeFlow(today - dayOfMonth + 1, today)
        ) { record, totalLast7Days, totalThisMonth ->
            StatsData(
                recordSteps = numberFormat.format(record.steps),
                recordDate = dateFormat.format(DateUtil.dayToLocalDate(record.day)),
                totalStepsLast7Days = numberFormat.format(totalLast7Days),
                averageStepsLast7Days = numberFormat.format(totalLast7Days / 7),
                totalStepsThisMonth = numberFormat.format(totalThisMonth),
                averageStepsThisMonth = numberFormat.format(totalThisMonth / dayOfMonth),
            )
        }
    }
}
