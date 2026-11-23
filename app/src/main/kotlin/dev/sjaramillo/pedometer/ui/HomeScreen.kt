package dev.sjaramillo.pedometer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import kotlin.math.abs

internal data class Palette(
    val background: Color,
    val surface: Color,
    val text: Color,
    val accent: Color,
    val track: Color,
    val bar: Color,
)

private val LightPalette =
    Palette(Color(0xFFF5F7F3), Color.White, Color(0xFF1B261E), Color(0xFF23633C), Color(0xFFD6E4D9), Color(0xFF49728C))
private val DarkPalette =
    Palette(Color(0xFF111914), Color(0xFF1C2920), Color(0xFFE6F0E7), Color(0xFF9BDAAA), Color(0xFF35483A), Color(0xFF8EB4CD))
internal val LocalPalette = staticCompositionLocalOf { LightPalette }

@Composable
internal fun HomeScreen(
    state: HomeUiState,
    showPrivacy: Boolean,
    onDismissPrivacy: () -> Unit,
    onConnect: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onRefresh: () -> Unit,
    onSaveGoal: (String) -> Unit,
    onDismissFileMessage: () -> Unit,
) {
    val file = state.file
    val palette = if (isSystemInDarkTheme()) DarkPalette else LightPalette
    var settings by rememberSaveable { mutableStateOf(false) }
    CompositionLocalProvider(LocalPalette provides palette) {
        Column(Modifier.fillMaxSize().background(palette.background).safeDrawingPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppText(stringResource(R.string.app_name), Modifier.weight(1f).semantics { heading() }, size = 22, bold = true)
                TextAction(stringResource(R.string.settings_title), onClick = { settings = true })
            }
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                TodayChart(state)
                SyncStatus(state.sync, onConnect, onRefresh)
                WeekChart(state)
                StatsSummary(state.bestDay, state.lifetimeSteps)
            }
        }
        when {
            showPrivacy ->
                AppDialog(stringResource(R.string.privacy_title), onDismissPrivacy) {
                    AppText(stringResource(R.string.health_connect_privacy_policy))
                    TextAction(stringResource(R.string.close), onClick = onDismissPrivacy)
                }

            settings ->
                SettingsSheet(
                    goal = state.goal,
                    file = file,
                    onDismiss = {
                        settings = false
                        if (file.message != null) onDismissFileMessage()
                    },
                    onSaveGoal = onSaveGoal,
                    onImport = onImport,
                    onExport = onExport,
                    onDismissFileMessage = onDismissFileMessage,
                    version = state.version,
                )
        }
        if (!settings && !showPrivacy) {
            file.message?.let { message ->
                AppDialog(stringResource(R.string.backup_result), onDismissFileMessage) {
                    AppText(message)
                    TextAction(stringResource(R.string.close), onClick = onDismissFileMessage)
                }
            }
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

@Composable
private fun SettingsSheet(
    goal: Int,
    file: FileState,
    onDismiss: () -> Unit,
    onSaveGoal: (String) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onDismissFileMessage: () -> Unit,
    version: String,
) {
    var draft by rememberSaveable { mutableStateOf<Int?>(null) }
    var about by rememberSaveable { mutableStateOf(false) }
    val dismiss = {
        when {
            draft != null -> draft = null
            about -> about = false
            else -> onDismiss()
        }
    }
    AppDialog(stringResource(R.string.settings_title), dismiss, bottomAligned = true) {
        val input = draft
        when {
            input != null -> {
                AppText(stringResource(R.string.set_goal), Modifier.semantics { heading() }, size = 20, bold = true)
                AppText(
                    stringResource(R.string.selected_goal, input),
                    Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    size = 24,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GoalAdjustment(input, -1_000) { draft = it }
                    GoalAdjustment(input, -100) { draft = it }
                    GoalAdjustment(input, 100) { draft = it }
                    GoalAdjustment(input, 1_000) { draft = it }
                }
                TextAction(stringResource(R.string.save), icon = "✓") {
                    onSaveGoal(input.toString())
                    draft = null
                }
                TextAction(stringResource(android.R.string.cancel), onClick = dismiss)
            }

            about -> {
                AppText(stringResource(R.string.about_text))
                AppText(stringResource(R.string.about_app_version, version))
                val uriHandler = LocalUriHandler.current
                TextAction(stringResource(R.string.original_author_site)) {
                    uriHandler.openUri("https://www.j4velin.de")
                }
                TextAction(stringResource(R.string.source_code)) {
                    uriHandler.openUri("https://github.com/sjaramillo10/Pedometer")
                }
                TextAction(stringResource(R.string.back), onClick = dismiss)
            }

            else -> {
                AppText(stringResource(R.string.goal_summary, goal))
                TextAction(stringResource(R.string.set_goal), onClick = { draft = goal })
                TextAction(stringResource(R.string.import_title), !file.busy, onClick = onImport)
                AppText(stringResource(R.string.import_summary))
                TextAction(stringResource(R.string.export_title), !file.busy, onClick = onExport)
                AppText(stringResource(R.string.export_summary))
                if (file.busy) {
                    AppText(stringResource(R.string.backup_working), Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
                file.message?.let { message ->
                    AppText(message, Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    TextAction(stringResource(R.string.dismiss_result), onClick = onDismissFileMessage)
                }
                TextAction(stringResource(R.string.about), onClick = { about = true })
                TextAction(stringResource(R.string.close), onClick = onDismiss)
            }
        }
    }
}

@Composable
private fun GoalAdjustment(
    goal: Int,
    amount: Int,
    onAdjust: (Int) -> Unit,
) {
    val nextGoal = adjustGoal(goal, amount)
    val enabled = nextGoal != goal
    val increasing = amount > 0
    val label = stringResource(if (increasing) R.string.increase_goal else R.string.decrease_goal, abs(amount))
    val glyph = if (increasing) "⌃" else "⌄"
    Box(
        modifier =
            Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .background(LocalPalette.current.track, RoundedCornerShape(8.dp))
                .semantics { contentDescription = label }
                .clickable(enabled = enabled, role = Role.Button) { onAdjust(nextGoal) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.clearAndSetSemantics {},
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy((-8).dp),
        ) {
            AppText(glyph, size = 20, bold = true, accent = enabled)
            if (abs(amount) == 1_000) {
                AppText(glyph, size = 20, bold = true, accent = enabled)
            }
        }
    }
}

@Composable
private fun AppDialog(
    title: String,
    onDismiss: () -> Unit,
    bottomAligned: Boolean = false,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties =
            DialogProperties(
                usePlatformDefaultWidth = !bottomAligned,
                decorFitsSystemWindows = !bottomAligned,
            ),
    ) {
        val palette = LocalPalette.current
        Box(
            modifier = if (bottomAligned) Modifier.fillMaxSize() else Modifier,
            contentAlignment = if (bottomAligned) Alignment.BottomCenter else Alignment.Center,
        ) {
            if (bottomAligned) {
                Box(Modifier.fillMaxSize().clickable(onClick = onDismiss).clearAndSetSemantics {})
            }
            Column(
                Modifier
                    .safeDrawingPadding()
                    .imePadding()
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(20.dp))
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AppText(title, Modifier.semantics { heading() }, size = 24, bold = true)
                content()
            }
        }
    }
}

@Composable
internal fun AppText(
    text: String,
    modifier: Modifier = Modifier,
    size: Int = 16,
    bold: Boolean = false,
    accent: Boolean = false,
) {
    val palette = LocalPalette.current
    BasicText(
        text,
        modifier,
        style =
            TextStyle(
                color = if (accent) palette.accent else palette.text,
                fontSize = size.sp,
                fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            ),
    )
}

@Composable
private fun TextAction(
    label: String,
    enabled: Boolean = true,
    icon: String? = null,
    onClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (icon == null) {
            AppText(label, bold = true, accent = enabled)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                AppText(icon, Modifier.clearAndSetSemantics {}, bold = true, accent = enabled)
                AppText(label, bold = true, accent = enabled)
            }
        }
    }
}
