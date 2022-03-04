package dev.sjaramillo.pedometer.ui.stats

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.util.DateUtil
import dev.sjaramillo.pedometer.util.FormatUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val stepsRepository: StepsRepository
) : ViewModel() {

    private val numberFormat = FormatUtil.numberFormat
    private val dateFormat = FormatUtil.dateFormat

    fun getStatsDataFlow(): Flow<StatsData> = flow {
        val today = DateUtil.getToday()
        val dayOfMonth = DateUtil.getDayOfMonth()
        val dayOfYear = DateUtil.getDayOfYear()
        val totalDays = stepsRepository.getTotalDays()

        val record = stepsRepository.getRecord()
        val totalStepsPrevious6Days = stepsRepository.getStepsFromDayRange(today - 6, today - 1)
        val totalStepsThisMonthUntilToday =
            stepsRepository.getStepsFromDayRange(today - dayOfMonth + 1, today - 1)
        val totalStepsThisYearUntilToday =
            stepsRepository.getStepsFromDayRange(today - dayOfYear + 1, today - 1)
        val totalStepsUntilToday = stepsRepository.getStepsFromDayRange(0, today - 1)

        stepsRepository.getStepsTodayFlow().collect { stepsToday ->
            val totalStepsLast7Days = totalStepsPrevious6Days + stepsToday
            val totalStepsThisMonth = totalStepsThisMonthUntilToday + stepsToday
            val totalStepsThisYear = totalStepsThisYearUntilToday + stepsToday
            val totalStepsAllTime = totalStepsUntilToday + stepsToday

            emit(
                StatsData(
                    recordSteps = numberFormat.format(record.steps),
                    recordDate = dateFormat.format(DateUtil.dayToLocalDate(record.day)),
                    totalStepsLast7Days = numberFormat.format(totalStepsLast7Days),
                    averageStepsLast7Days = numberFormat.format(totalStepsLast7Days / 7),
                    totalStepsThisMonth = numberFormat.format(totalStepsThisMonth),
                    averageStepsThisMonth = numberFormat.format(totalStepsThisMonth / dayOfMonth),
                    totalStepsThisYear = numberFormat.format(totalStepsThisYear),
                    averageStepsThisYear = numberFormat.format(totalStepsThisYear / dayOfYear),
                    totalStepsAllTime = numberFormat.format(totalStepsAllTime),
                    averageStepsAllTime = numberFormat.format(totalStepsAllTime / totalDays),
                )
            )
        }
    }
}
