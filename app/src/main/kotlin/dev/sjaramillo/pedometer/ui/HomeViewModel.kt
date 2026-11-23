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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
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

    private val fileState = MutableStateFlow(FileState())
    private val version =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    val uiState =
        observeHomeUiState(
            date,
            stepsRepository::observeRecentDays,
            goal,
            coordinator.state,
            fileState,
            stepsRepository.observeAll(),
            version,
        ).stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            HomeUiState(HomeTotals(date.value), goal = goal.value, version = version),
        )

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
        fileState.value = fileState.value.copy(message = null)
    }

    private fun runFileOperation(operation: suspend () -> String) {
        if (fileState.value.busy) return
        fileState.value = FileState(busy = true)
        viewModelScope.launch {
            val message =
                try {
                    operation()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    context.getString(R.string.error_file, error.localizedMessage.orEmpty())
                }
            fileState.value = FileState(message = message)
        }
    }
}

data class FileState(val busy: Boolean = false, val message: String? = null)

data class HomeUiState(
    val totals: HomeTotals,
    val goal: Int = DEFAULT_GOAL,
    val sync: HealthConnectSyncState = HealthConnectSyncState.Loading,
    val file: FileState = FileState(),
    val bestDay: DailySteps? = null,
    val lifetimeSteps: Long? = null,
    val version: String = "",
) {
    val progress: Float
        get() = ((totals.today?.steps ?: 0L).toDouble() / goal).coerceIn(0.0, 1.0).toFloat()

    fun reachedGoal(steps: Long): Boolean = steps >= goal
}

data class HomeTotals(val date: LocalDate, val entries: List<DailySteps> = emptyList()) {
    val today: DailySteps?
        get() = entries.firstOrNull { it.day == date.toEpochDay() }

    internal fun currentWeek(firstDay: DayOfWeek): List<WeekDay> {
        val start = date.with(TemporalAdjusters.previousOrSame(firstDay))
        val byDay = entries.associateBy(DailySteps::day)
        return List(7) { index ->
            val day = start.plusDays(index.toLong())
            WeekDay(day, byDay[day.toEpochDay()]?.takeUnless { day.isAfter(date) })
        }
    }
}

internal data class WeekDay(val date: LocalDate, val total: DailySteps?)

internal data class WeekSummary(val total: Long, val average: Long)

internal fun summarizeWeek(
    days: List<WeekDay>,
    today: LocalDate,
): WeekSummary {
    val elapsed = days.filter { it.date <= today }
    val total = elapsed.sumOf { it.total?.steps ?: 0L }
    return WeekSummary(total, total / elapsed.size.coerceAtLeast(1))
}

private const val DEFAULT_GOAL = 10_000

internal fun parseGoal(input: String): Int? = input.trim().toIntOrNull()?.takeIf { it in 1..100_000 }

internal fun observeHomeUiState(
    dates: Flow<LocalDate>,
    observeRecentDays: (LocalDate) -> Flow<List<DailySteps>>,
    goals: Flow<Int>,
    sync: Flow<HealthConnectSyncState>,
    files: Flow<FileState>,
    history: Flow<List<DailySteps>>,
    version: String = "",
): Flow<HomeUiState> =
    combine(observeHomeTotals(dates, observeRecentDays), goals, sync, files, history) {
        totals,
        goal,
        syncState,
        file,
        entries,
        ->
        HomeUiState(
            totals = totals,
            goal = goal,
            sync = syncState,
            file = file,
            bestDay = entries.maxByOrNull(DailySteps::steps),
            lifetimeSteps = entries.sumOf(DailySteps::steps),
            version = version,
        )
    }

@OptIn(ExperimentalCoroutinesApi::class)
private fun observeHomeTotals(
    dates: Flow<LocalDate>,
    observeRecentDays: (LocalDate) -> Flow<List<DailySteps>>,
): Flow<HomeTotals> =
    dates.flatMapLatest { date ->
        observeRecentDays(date).map { entries -> HomeTotals(date, entries) }
    }
