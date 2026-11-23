package dev.sjaramillo.pedometer.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectSyncCoordinator @Inject constructor(
    private val stepsProvider: StepsProvider,
    private val stepCacheSynchronizer: StepCacheSynchronizer,
) {
    private val _state = MutableStateFlow<HealthConnectSyncState>(HealthConnectSyncState.Loading)
    val state: StateFlow<HealthConnectSyncState> = _state.asStateFlow()

    suspend fun refresh() {
        _state.value = HealthConnectSyncState.Loading
        try {
            when (stepsProvider.access()) {
                StepsAccess.PermissionRequired -> _state.value = HealthConnectSyncState.PermissionRequired
                StepsAccess.Unavailable -> _state.value = HealthConnectSyncState.Unavailable
                StepsAccess.Available -> {
                    _state.value = HealthConnectSyncState.Syncing
                    stepCacheSynchronizer.sync()
                    _state.value = HealthConnectSyncState.Ready
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            _state.value = HealthConnectSyncState.Error
        }
    }

    companion object {
        val READ_STEPS_PERMISSION = dev.sjaramillo.pedometer.data.READ_STEPS_PERMISSION
    }
}

enum class HealthConnectSyncState {
    Loading,
    PermissionRequired,
    Unavailable,
    Syncing,
    Ready,
    Error,
}
