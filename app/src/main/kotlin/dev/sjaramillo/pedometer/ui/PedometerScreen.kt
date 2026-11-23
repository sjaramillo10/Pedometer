package dev.sjaramillo.pedometer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import dev.sjaramillo.pedometer.ui.stats.StatsViewModel

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
fun PedometerScreen(
    home: HomeViewModel,
    stats: StatsViewModel,
    showPrivacy: Boolean,
    onDismissPrivacy: () -> Unit,
    onConnect: () -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    version: String,
) {
    val state by home.state.collectAsStateWithLifecycle()
    val file by home.fileState.collectAsStateWithLifecycle()
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
                SyncStatus(state.sync, onConnect, home::refresh)
                WeekChart(state)
                StatsSummary(stats)
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
                        if (file.message != null) home.dismissFileMessage()
                    },
                    onSaveGoal = home::saveGoal,
                    onImport = onImport,
                    onExport = onExport,
                    onDismissFileMessage = home::dismissFileMessage,
                    version = version,
                )
        }
        if (!settings && !showPrivacy) {
            file.message?.let { message ->
                AppDialog(stringResource(R.string.backup_result), home::dismissFileMessage) {
                    AppText(message)
                    TextAction(stringResource(R.string.close), onClick = home::dismissFileMessage)
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
    var draft by rememberSaveable { mutableStateOf<String?>(null) }
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
                val valid = parseGoal(input) != null
                val hint = stringResource(R.string.goal_range)
                AppText(stringResource(R.string.set_goal), Modifier.semantics { heading() }, size = 20, bold = true)
                AppText(hint)
                BasicTextField(
                    value = input,
                    onValueChange = { draft = it },
                    textStyle = TextStyle(color = LocalPalette.current.text, fontSize = 24.sp),
                    cursorBrush = SolidColor(LocalPalette.current.accent),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .background(LocalPalette.current.track, RoundedCornerShape(8.dp))
                            .padding(12.dp)
                            .semantics {
                                if (!valid) error(hint)
                                stateDescription = hint
                            },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                )
                TextAction(stringResource(R.string.save), enabled = valid) {
                    onSaveGoal(input)
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
                TextAction(stringResource(R.string.set_goal), onClick = { draft = goal.toString() })
                TextAction(stringResource(R.string.import_title), !file.busy, onImport)
                AppText(stringResource(R.string.import_summary))
                TextAction(stringResource(R.string.export_title), !file.busy, onExport)
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
        AppText(label, bold = true, accent = enabled)
    }
}
