package dev.sjaramillo.pedometer.data

import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import kotlinx.coroutines.flow.StateFlow

interface HealthConnectSyncCoordinator {
    val state: StateFlow<HealthConnectSyncState>

    suspend fun refresh()

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
