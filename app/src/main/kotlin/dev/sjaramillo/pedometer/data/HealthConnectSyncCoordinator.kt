package dev.sjaramillo.pedometer.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HealthConnectSyncCoordinator internal constructor(
    private val checkAccess: suspend () -> HealthConnectAccess,
    private val syncCache: suspend () -> Unit,
) {
    @Inject
    constructor(
        phoneStepsDataSource: PhoneStepsDataSource,
        cacheSynchronizer: HealthConnectStepCacheSynchronizer,
    ) : this(phoneStepsDataSource::access, cacheSynchronizer::sync)

    private val _state = MutableStateFlow<HealthConnectSyncState>(HealthConnectSyncState.Loading)
    val state: StateFlow<HealthConnectSyncState> = _state.asStateFlow()

    private val refreshMutex = Mutex()

    suspend fun refresh() =
        refreshMutex.withLock {
            _state.value = HealthConnectSyncState.Loading
            try {
                when (checkAccess()) {
                    HealthConnectAccess.PermissionRequired -> _state.value = HealthConnectSyncState.PermissionRequired
                    HealthConnectAccess.Unavailable -> _state.value = HealthConnectSyncState.Unavailable
                    HealthConnectAccess.Available -> {
                        _state.value = HealthConnectSyncState.Syncing
                        syncCache()
                        _state.value = HealthConnectSyncState.Ready
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                _state.value = HealthConnectSyncState.Error
            }
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
