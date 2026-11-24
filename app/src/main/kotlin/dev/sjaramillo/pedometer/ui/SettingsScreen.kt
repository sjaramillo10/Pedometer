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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
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
    val safeDrawing = WindowInsets.safeDrawing
    Column(
        Modifier
            .fillMaxSize()
            .background(LocalPalette.current.background)
            .windowInsetsPadding(safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
    ) {
        ScreenToolbar(stringResource(R.string.settings_title), actionAtStart = true) {
            ToolbarAction(R.drawable.ic_back, stringResource(R.string.back), onBack)
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(safeDrawing.only(WindowInsetsSides.Bottom))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(stringResource(R.string.daily_step_goal)) {
                GoalControl(state.goal, onAdjustGoal)
            }
            SectionCard(stringResource(R.string.appearance)) {
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
            SectionCard(stringResource(R.string.backup)) {
                BackupRow(R.string.import_title, R.string.import_summary, !file.busy, onImport)
                SectionDivider()
                BackupRow(R.string.export_title, R.string.export_summary, !file.busy, onExport)
                SectionDivider()
                AppText(
                    stringResource(R.string.backup_precedence),
                    Modifier.padding(top = 12.dp),
                    size = 13,
                    secondary = true,
                )
                if (file.busy) {
                    AppText(stringResource(R.string.backup_working), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                file.message?.let { message ->
                    AppText(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    TextAction(stringResource(R.string.dismiss_result), onClick = onDismissFileMessage)
                }
            }
            SectionCard(stringResource(R.string.about)) {
                AboutDetails(state.version)
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
    SectionCard(stringResource(R.string.health_connect), modifier) {
        val palette = LocalPalette.current
        val message =
            when (status) {
                HealthConnectSyncState.Loading, HealthConnectSyncState.Syncing -> R.string.health_connect_syncing
                HealthConnectSyncState.PermissionRequired -> R.string.health_connect_disconnected
                HealthConnectSyncState.Unavailable -> R.string.health_connect_unavailable
                HealthConnectSyncState.Error -> R.string.health_connect_error
                HealthConnectSyncState.Ready -> R.string.health_connect_connected
            }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AppText(stringResource(R.string.health_connect_description), size = 14, secondary = true)
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(palette.background, RoundedCornerShape(16.dp))
                    .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val connected = status == HealthConnectSyncState.Ready
                Box(Modifier.size(8.dp).background(if (connected) palette.accent else palette.text.copy(alpha = 0.55f), CircleShape))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AppText(stringResource(R.string.health_connect_status), size = 13, bold = true)
                    if (!connected) AppText(stringResource(message), size = 13, secondary = true)
                }
                if (connected) {
                    AppText(
                        stringResource(message),
                        Modifier.background(palette.track, CircleShape).padding(horizontal = 10.dp, vertical = 4.dp),
                        size = 13,
                        bold = true,
                        accent = true,
                    )
                }
            }
            when (status) {
                HealthConnectSyncState.PermissionRequired ->
                    TextAction(
                        stringResource(R.string.connect_health_connect),
                        onClick = onConnect,
                    )
                HealthConnectSyncState.Error -> TextAction(stringResource(R.string.retry), onClick = onRetry)
                else -> Unit
            }
        }
        SettingsLink(
            R.string.learn_health_connect,
            "https://developer.android.com/health-and-fitness/health-connect/get-started",
            external = false,
        )
    }
}

@Composable
private fun ThemeOptions(
    theme: AppTheme,
    onSetTheme: (AppTheme) -> Unit,
) {
    val palette = LocalPalette.current
    Column(Modifier.selectableGroup()) {
        for ((index, option) in AppTheme.entries.withIndex()) {
            if (index > 0) SectionDivider()
            val selected = option == theme
            Row(
                Modifier
                    .fillMaxWidth()
                    .sizeIn(minHeight = 56.dp)
                    .selectable(selected = selected, role = Role.RadioButton) { onSetTheme(option) }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(stringResource(option.label), Modifier.weight(1f), size = 15)
                Box(
                    Modifier.size(20.dp).border(
                        if (selected) 7.dp else 1.dp,
                        if (selected) palette.accent else palette.text.copy(alpha = 0.40f),
                        CircleShape,
                    ),
                )
            }
        }
    }
}

@Composable
private fun BackupRow(
    title: Int,
    summary: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 60.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            AppText(stringResource(title), size = 15, bold = true, accent = enabled, secondary = !enabled)
            AppText(stringResource(summary), size = 13, secondary = true)
        }
        AppText("›", Modifier.clearAndSetSemantics {}, size = 22, secondary = true)
    }
}

@Composable
private fun AboutDetails(version: String) {
    AppText(
        stringResource(R.string.about_app_version, version),
        Modifier.fillMaxWidth().padding(bottom = 16.dp),
        size = 13,
        secondary = true,
        align = TextAlign.Center,
    )
    AppText(stringResource(R.string.about_text), size = 14)
    SettingsLink(R.string.original_author_site, "https://www.j4velin.de")
    SectionDivider()
    SettingsLink(R.string.source_code, "https://github.com/sjaramillo10/Pedometer")
}

@Composable
private fun SettingsLink(
    label: Int,
    url: String,
    external: Boolean = true,
) {
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clickable(role = Role.Button) { uriHandler.openUri(url) }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(stringResource(label), size = 14, bold = true, accent = true)
        AppText(if (external) "↗" else "›", Modifier.clearAndSetSemantics {}, size = 16, accent = true)
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
            AppText(stringResource(R.string.selected_goal, goal), size = 24, bold = true, align = TextAlign.Center)
            AppText(stringResource(R.string.steps_per_day), size = 14, secondary = true, align = TextAlign.Center)
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
        AppText(
            if (increasing) "+" else "−",
            Modifier.clearAndSetSemantics {},
            size = 24,
            bold = true,
            accent = enabled,
            secondary = !enabled,
        )
    }
}
