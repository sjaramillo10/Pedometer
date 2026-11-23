package dev.sjaramillo.pedometer.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.HealthConnectSyncState

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    scroll: ScrollState,
    onOpenSettings: () -> Unit,
    onConnect: () -> Unit,
    onRefresh: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(LocalPalette.current.background).safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(stringResource(R.string.app_name), Modifier.weight(1f).semantics { heading() }, size = 22, bold = true)
            ToolbarAction(R.drawable.ic_settings, stringResource(R.string.settings_title), onOpenSettings)
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(scroll).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TodayChart(state)
            SyncStatus(state.sync, onConnect, onRefresh)
            WeekChart(state)
            StatsSummary(state.bestDay, state.lifetimeSteps)
        }
    }
}

@Composable
private fun SyncStatus(
    status: HealthConnectSyncState,
    onConnect: () -> Unit,
    onRetry: () -> Unit,
) {
    val message =
        when (status) {
            HealthConnectSyncState.Loading, HealthConnectSyncState.Syncing -> R.string.health_connect_syncing
            HealthConnectSyncState.PermissionRequired -> R.string.health_connect_permission_required
            HealthConnectSyncState.Unavailable -> R.string.health_connect_unavailable
            HealthConnectSyncState.Error -> R.string.health_connect_error
            HealthConnectSyncState.Ready -> return
        }
    Column {
        AppText(stringResource(message), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        when (status) {
            HealthConnectSyncState.PermissionRequired -> TextAction(stringResource(R.string.connect_health_connect), onClick = onConnect)
            HealthConnectSyncState.Error -> TextAction(stringResource(R.string.retry), onClick = onRetry)
            else -> Unit
        }
    }
}
