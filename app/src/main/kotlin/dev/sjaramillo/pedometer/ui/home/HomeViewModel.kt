package dev.sjaramillo.pedometer.ui.home

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.util.DateUtil
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val stepsRepository: StepsRepository
) : ViewModel() {

    fun getStepsToday(): Long {
        return stepsRepository.getStepsToday()
    }

    suspend fun getTotalSteps(): Long {
        return stepsRepository.getStepsFromDayRange(0, DateUtil.getToday())
    }

    fun updateStepsSinceBoot(steps: Long): Long {
        return stepsRepository.updateStepsSinceBoot(steps)
    }

    fun getLastEntries(num: Int): List<DailySteps> {
        return stepsRepository.getLastEntries(num)
    }
}
