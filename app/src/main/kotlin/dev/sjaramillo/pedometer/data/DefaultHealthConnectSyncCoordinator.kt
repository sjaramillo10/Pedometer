package dev.sjaramillo.pedometer.data

import androidx.health.connect.client.HealthConnectClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultHealthConnectSyncCoordinator @Inject constructor(
    private val healthConnectClient: HealthConnectClient,
    private val stepCacheSynchronizer: HealthConnectStepCacheSynchronizer,
) : HealthConnectSyncCoordinator {
    private val _state = MutableStateFlow<HealthConnectSyncState>(HealthConnectSyncState.Loading)
    override val state: StateFlow<HealthConnectSyncState> = _state.asStateFlow()

    override suspend fun refresh() {
        if (HealthConnectSyncCoordinator.READ_STEPS_PERMISSION !in
            healthConnectClient.permissionController.getGrantedPermissions()
        ) {
            _state.value = HealthConnectSyncState.PermissionRequired
            return
        }

        _state.value = HealthConnectSyncState.Syncing
        runCatching { stepCacheSynchronizer.sync() }
            .onSuccess { _state.value = HealthConnectSyncState.Ready }
            .onFailure { _state.value = HealthConnectSyncState.Error }
    }
}
