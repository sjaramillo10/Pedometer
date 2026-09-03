package dev.sjaramillo.pedometer.data

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectSyncCoordinator @Inject constructor(
    private val healthConnectClient: HealthConnectClient,
    private val stepCacheSynchronizer: HealthConnectStepCacheSynchronizer,
) {
    private val _state = MutableStateFlow<HealthConnectSyncState>(HealthConnectSyncState.Loading)
    val state: StateFlow<HealthConnectSyncState> = _state.asStateFlow()

    suspend fun refresh() {
        if (READ_STEPS_PERMISSION !in healthConnectClient.permissionController.getGrantedPermissions()) {
            _state.value = HealthConnectSyncState.PermissionRequired
            return
        }

        _state.value = HealthConnectSyncState.Syncing
        runCatching { stepCacheSynchronizer.sync() }
            .onSuccess { _state.value = HealthConnectSyncState.Ready }
            .onFailure { _state.value = HealthConnectSyncState.Error }
    }

    companion object {
        val READ_STEPS_PERMISSION = HealthPermission.getReadPermission(StepsRecord::class)
    }
}

enum class HealthConnectSyncState {
    Loading,
    PermissionRequired,
    Syncing,
    Ready,
    Error,
}
