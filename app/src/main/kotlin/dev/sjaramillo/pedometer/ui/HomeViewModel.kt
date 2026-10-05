package dev.sjaramillo.pedometer.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.StepsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(dailySteps: Flow<List<DailySteps>>) : ViewModel() {
    val entries: StateFlow<List<DailySteps>> =
        dailySteps.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
}

class HomeViewModelFactory(
    private val stepsRepository: StepsRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(stepsRepository.observeAll()) as T
    }
}
