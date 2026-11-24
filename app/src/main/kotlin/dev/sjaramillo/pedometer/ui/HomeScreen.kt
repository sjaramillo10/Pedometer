package dev.sjaramillo.pedometer.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
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
    onOpenHealthConnect: () -> Unit,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val safeDrawing = WindowInsets.safeDrawing
    Column(
        Modifier
            .fillMaxSize()
            .background(LocalPalette.current.background)
            .windowInsetsPadding(safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppText(stringResource(R.string.app_name), Modifier.weight(1f).semantics { heading() }, size = 22, bold = true)
            ToolbarAction(R.drawable.ic_settings, stringResource(R.string.settings_title), onOpenSettings)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .windowInsetsPadding(safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TodayChart(state)
            if (state.sync == HealthConnectSyncState.PermissionRequired) {
                AppText(
                    stringResource(R.string.health_connect_permission_required),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                TextAction(stringResource(R.string.open_settings), onClick = onOpenHealthConnect)
            }
            WeekChart(state, onPreviousWeek, onNextWeek)
            StatsSummary(state.bestDay, state.lifetimeSteps)
        }
    }
}
