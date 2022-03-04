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

        val record = stepsRepository.getRecord()
        val totalLast6Days = stepsRepository.getStepsFromDayRange(today - 6, today - 1)
        val almostThisMonth =
            stepsRepository.getStepsFromDayRange(today - dayOfMonth + 1, today - 1)
        val almostThisYear = stepsRepository.getStepsFromDayRange(today - dayOfYear + 1, today - 1)

        stepsRepository.getStepsTodayFlow().collect { stepsToday ->
            val totalLast7Days = totalLast6Days + stepsToday
            val totalThisMonth = almostThisMonth + stepsToday
            val totalThisYear = almostThisYear + stepsToday

            emit(
                StatsData(
                    recordSteps = numberFormat.format(record.steps),
                    recordDate = dateFormat.format(DateUtil.dayToLocalDate(record.day)),
                    totalStepsLast7Days = numberFormat.format(totalLast7Days),
                    averageStepsLast7Days = numberFormat.format(totalLast7Days / 7),
                    totalStepsThisMonth = numberFormat.format(totalThisMonth),
                    averageStepsThisMonth = numberFormat.format(totalThisMonth / dayOfMonth),
                    totalStepsThisYear = numberFormat.format(totalThisYear),
                    averageStepsThisYear = numberFormat.format(totalThisYear / dayOfYear)
                )
            )
        }
    }
}
