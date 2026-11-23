package dev.sjaramillo.pedometer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import kotlin.math.abs

internal const val FIRST_GOAL_REPEAT_DELAY = 150L

internal fun nextGoalRepeatDelay(delayMillis: Long): Long = (delayMillis - 15L).coerceAtLeast(45L)

@Composable
internal fun SettingsScreen(
    state: SettingsUiState,
    revealHealthConnect: Boolean,
    onHealthConnectRevealed: () -> Unit,
    onBack: () -> Unit,
    onConnect: () -> Unit,
    onRetry: () -> Unit,
    onAdjustGoal: (Int) -> Unit,
    onSetTheme: (AppTheme) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onDismissFileMessage: () -> Unit,
) {
    val file = state.file
    val healthConnectRequester = remember { BringIntoViewRequester() }
    var healthConnectPlaced by remember { mutableStateOf(false) }
    LaunchedEffect(revealHealthConnect, healthConnectPlaced) {
        if (revealHealthConnect && healthConnectPlaced) {
            healthConnectRequester.bringIntoView()
            onHealthConnectRevealed()
        }
    }
    Column(Modifier.fillMaxSize().background(LocalPalette.current.background).safeDrawingPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToolbarAction(R.drawable.ic_back, stringResource(R.string.back), onBack)
            AppText(stringResource(R.string.settings_title), Modifier.semantics { heading() }, size = 22, bold = true)
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SettingsCard(stringResource(R.string.daily_step_goal)) {
                GoalControl(state.goal, onAdjustGoal)
            }
            SettingsCard(stringResource(R.string.appearance)) {
                ThemeOptions(state.theme, onSetTheme)
            }
            HealthConnectCard(
                state.sync,
                onConnect,
                onRetry,
                Modifier
                    .bringIntoViewRequester(healthConnectRequester)
                    .onGloballyPositioned { healthConnectPlaced = true },
            )
            SettingsCard(stringResource(R.string.backup)) {
                Column {
                    TextAction(stringResource(R.string.import_title), !file.busy, onClick = onImport)
                    AppText(stringResource(R.string.import_summary))
                }
                Column {
                    TextAction(stringResource(R.string.export_title), !file.busy, onClick = onExport)
                    AppText(stringResource(R.string.export_summary))
                }
                if (file.busy) {
                    AppText(stringResource(R.string.backup_working), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                file.message?.let { message ->
                    AppText(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    TextAction(stringResource(R.string.dismiss_result), onClick = onDismissFileMessage)
                }
            }
            SettingsCard(stringResource(R.string.about)) {
                AppText(stringResource(R.string.about_text))
                AppText(stringResource(R.string.about_app_version, state.version))
                val uriHandler = LocalUriHandler.current
                TextAction(stringResource(R.string.original_author_site)) {
                    uriHandler.openUri("https://www.j4velin.de")
                }
                TextAction(stringResource(R.string.source_code)) {
                    uriHandler.openUri("https://github.com/sjaramillo10/Pedometer")
                }
            }
        }
    }
}

@Composable
private fun HealthConnectCard(
    status: HealthConnectSyncState,
    onConnect: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier,
) {
    SettingsCard(stringResource(R.string.health_connect), modifier) {
        AppText(stringResource(R.string.health_connect_description))
        val message =
            when (status) {
                HealthConnectSyncState.Loading, HealthConnectSyncState.Syncing -> R.string.health_connect_syncing
                HealthConnectSyncState.PermissionRequired -> R.string.health_connect_disconnected
                HealthConnectSyncState.Unavailable -> R.string.health_connect_unavailable
                HealthConnectSyncState.Error -> R.string.health_connect_error
                HealthConnectSyncState.Ready -> R.string.health_connect_connected
            }
        AppText(stringResource(message), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        when (status) {
            HealthConnectSyncState.PermissionRequired -> TextAction(stringResource(R.string.connect_health_connect), onClick = onConnect)
            HealthConnectSyncState.Error -> TextAction(stringResource(R.string.retry), onClick = onRetry)
            else -> Unit
        }
        val uriHandler = LocalUriHandler.current
        TextAction(stringResource(R.string.learn_health_connect)) {
            uriHandler.openUri("https://developer.android.com/health-and-fitness/health-connect/get-started")
        }
    }
}

@Composable
private fun ThemeOptions(
    theme: AppTheme,
    onSetTheme: (AppTheme) -> Unit,
) {
    val palette = LocalPalette.current
    Column(Modifier.selectableGroup()) {
        for (option in AppTheme.entries) {
            val selected = option == theme
            Row(
                Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 48.dp)
                    .selectable(selected = selected, role = Role.RadioButton) { onSetTheme(option) }
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(20.dp).border(2.dp, palette.accent, CircleShape), contentAlignment = Alignment.Center) {
                    if (selected) Box(Modifier.size(10.dp).background(palette.accent, CircleShape))
                }
                AppText(stringResource(option.label), bold = selected)
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Column(
        modifier.fillMaxWidth().background(LocalPalette.current.surface, RoundedCornerShape(24.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AppText(title, Modifier.semantics { heading() }, size = 20, bold = true)
        content()
    }
}

@Composable
private fun GoalControl(
    goal: Int,
    onAdjust: (Int) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(LocalPalette.current.track, RoundedCornerShape(16.dp))
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GoalAdjustment(goal, -100, onAdjust)
        Column(
            modifier = Modifier.weight(1f).semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppText(stringResource(R.string.selected_goal, goal), size = 24, bold = true)
            AppText(stringResource(R.string.steps_per_day), size = 14)
        }
        GoalAdjustment(goal, 100, onAdjust)
    }
}

@Composable
private fun GoalAdjustment(
    goal: Int,
    amount: Int,
    onAdjust: (Int) -> Unit,
) {
    val enabled = adjustGoal(goal, amount) != goal
    val increasing = amount > 0
    val label = stringResource(if (increasing) R.string.increase_goal else R.string.decrease_goal, abs(amount))
    val currentOnAdjust by rememberUpdatedState(onAdjust)
    Box(
        modifier =
            Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .background(LocalPalette.current.surface, CircleShape)
                .semantics { contentDescription = label }
                .clickable(enabled = enabled, role = Role.Button) { onAdjust(amount) }
                .pointerInput(enabled, amount) {
                    if (!enabled) return@pointerInput
                    awaitEachGesture {
                        val pass = PointerEventPass.Initial
                        awaitFirstDown(requireUnconsumed = false, pass = pass)
                        var held = false
                        var waitMillis = viewConfiguration.longPressTimeoutMillis
                        while (true) {
                            val finished =
                                withTimeoutOrNull(waitMillis) {
                                    val up = waitForUpOrCancellation(pass)
                                    // Let clickable handle short taps. After a repeat, consume the release.
                                    if (held) up?.consume()
                                    true
                                }
                            if (finished == true) break
                            currentOnAdjust(amount)
                            waitMillis = if (held) nextGoalRepeatDelay(waitMillis) else FIRST_GOAL_REPEAT_DELAY
                            held = true
                        }
                    }
                },
        contentAlignment = Alignment.Center,
    ) {
        AppText(if (increasing) "+" else "−", Modifier.clearAndSetSemantics {}, size = 24, bold = true, accent = enabled)
    }
}
