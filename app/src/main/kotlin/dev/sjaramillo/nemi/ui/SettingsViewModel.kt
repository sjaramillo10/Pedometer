package dev.sjaramillo.nemi.ui

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.sjaramillo.nemi.R
import dev.sjaramillo.nemi.data.HealthConnectSyncCoordinator
import dev.sjaramillo.nemi.data.HealthConnectSyncState
import dev.sjaramillo.nemi.data.StepsRepository
import dev.sjaramillo.nemi.data.decodeStepsCsv
import dev.sjaramillo.nemi.data.encodeStepsCsv
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class SettingsViewModel(
    private val context: Context,
    private val preferences: NemiPreferences,
    private val stepsRepository: StepsRepository,
    coordinator: HealthConnectSyncCoordinator,
) : ViewModel() {
    private val fileState = MutableStateFlow(FileState())
    private val version =
        context.packageManager
            .getPackageInfo(context.packageName, 0)
            .versionName
            .orEmpty()
    internal val uiState =
        combine(preferences.goal, preferences.theme, fileState, coordinator.state) { goal, theme, file, sync ->
            SettingsUiState(goal, theme, file, version, sync)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            SettingsUiState(preferences.goal.value, preferences.theme.value, fileState.value, version, coordinator.state.value),
        )

    fun adjustGoal(amount: Int) = preferences.adjustGoal(amount)

    fun saveTheme(theme: AppTheme) = preferences.setTheme(theme)

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

internal data class FileState(val busy: Boolean = false, val message: String? = null)

internal data class SettingsUiState(
    val goal: Int,
    val theme: AppTheme,
    val file: FileState,
    val version: String,
    val sync: HealthConnectSyncState,
)
