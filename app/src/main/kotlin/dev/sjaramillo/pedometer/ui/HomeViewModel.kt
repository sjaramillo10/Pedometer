package dev.sjaramillo.pedometer.ui

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.data.decodeStepsCsv
import dev.sjaramillo.pedometer.data.encodeStepsCsv
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val preferences: SharedPreferences,
    private val stepsRepository: StepsRepository,
    private val coordinator: HealthConnectSyncCoordinator,
) : ViewModel() {
    private val goal = MutableStateFlow(preferences.getInt("goal", DEFAULT_GOAL))
    private val date = MutableStateFlow(LocalDate.now())
    private var refreshJob: Job? = null

    val state =
        observeHomeState(date, stepsRepository::observeRecentDays, goal, coordinator.state)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState(goal = goal.value))
    private val _fileState = MutableStateFlow(FileState())
    val fileState = _fileState.asStateFlow()

    fun refresh() {
        val today = LocalDate.now()
        if (today == date.value && refreshJob?.isActive == true) return
        date.value = today
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch { coordinator.refresh() }
    }

    fun saveGoal(input: String) {
        val value = parseGoal(input) ?: return
        preferences.edit().putInt("goal", value).apply()
        goal.value = value
    }

    fun importCsv(uri: Uri) =
        runFileOperation {
            val decoded =
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                        decodeStepsCsv(it.readText())
                    } ?: throw IOException("Unable to open import source")
                }
            stepsRepository.importDailySteps(decoded.dailySteps)
            context.getString(R.string.entries_imported, decoded.dailySteps.size, decoded.ignoredRows)
        }

    fun exportCsv(uri: Uri) =
        runFileOperation {
            val totals = stepsRepository.getAll()
            withContext(Dispatchers.IO) {
                context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(encodeStepsCsv(totals))
                } ?: throw IOException("Unable to open export destination")
            }
            context.getString(R.string.data_exported)
        }

    fun dismissFileMessage() {
        _fileState.value = _fileState.value.copy(message = null)
    }

    private fun runFileOperation(operation: suspend () -> String) {
        if (_fileState.value.busy) return
        _fileState.value = FileState(busy = true)
        viewModelScope.launch {
            val message =
                try {
                    operation()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    context.getString(R.string.error_file, error.localizedMessage.orEmpty())
                }
            _fileState.value = FileState(message = message)
        }
    }
}

data class FileState(val busy: Boolean = false, val message: String? = null)

data class HomeState(
    val totals: HomeTotals = HomeTotals(),
    val goal: Int = DEFAULT_GOAL,
    val sync: HealthConnectSyncState = HealthConnectSyncState.Loading,
) {
    val progress: Float
        get() = ((totals.today?.steps ?: 0L).toDouble() / goal).coerceIn(0.0, 1.0).toFloat()

    fun reachedGoal(steps: Long): Boolean = steps >= goal
}

data class HomeTotals(val today: DailySteps? = null, val history: List<DailySteps> = emptyList())

private const val DEFAULT_GOAL = 10_000

internal fun parseGoal(input: String): Int? = input.trim().toIntOrNull()?.takeIf { it in 1..100_000 }

internal fun observeHomeState(
    dates: Flow<LocalDate>,
    observeRecentDays: (LocalDate) -> Flow<List<DailySteps>>,
    goals: Flow<Int>,
    sync: Flow<HealthConnectSyncState>,
): Flow<HomeState> = combine(observeHomeTotals(dates, observeRecentDays), goals, sync, ::HomeState)

@OptIn(ExperimentalCoroutinesApi::class)
private fun observeHomeTotals(
    dates: Flow<LocalDate>,
    observeRecentDays: (LocalDate) -> Flow<List<DailySteps>>,
): Flow<HomeTotals> =
    dates.flatMapLatest { date ->
        val today = date.toEpochDay()
        observeRecentDays(date).map { entries ->
            HomeTotals(entries.firstOrNull { it.day == today }, entries.filter { it.day < today }.asReversed())
        }
    }
