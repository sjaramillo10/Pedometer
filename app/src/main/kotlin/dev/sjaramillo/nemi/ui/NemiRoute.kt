package dev.sjaramillo.nemi.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.health.connect.HealthPermissions.READ_STEPS
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sjaramillo.nemi.R
import dev.sjaramillo.nemi.data.stepsCsvFileName
import java.time.LocalDate
import java.time.temporal.WeekFields

@Composable
internal fun NemiRoute(
    activity: ComponentActivity,
    initialIntent: Intent,
    incomingIntent: Intent?,
) {
    val models = remember(activity) { ViewModelProvider(activity) }
    val home = remember(models) { models[HomeViewModel::class.java] }
    val settings = remember(models) { models[SettingsViewModel::class.java] }
    val homeState by home.uiState.collectAsStateWithLifecycle()
    val settingsState by settings.uiState.collectAsStateWithLifecycle()
    val packageName = activity.packageName
    var showPrivacy by rememberSaveable { mutableStateOf(initialIntent.opensPrivacyPolicy(packageName)) }
    LaunchedEffect(incomingIntent) {
        incomingIntent?.let { showPrivacy = it.opensPrivacyPolicy(packageName) }
    }

    val permission =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            home.refresh(restart = true)
        }
    val importDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(settings::importCsv)
        }
    val exportDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let(settings::exportCsv)
        }

    LifecycleResumeEffect(home) {
        home.refresh()
        onPauseOrDispose {}
    }
    LifecycleStartEffect(activity, home) {
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(
                    context: Context,
                    intent: Intent,
                ) {
                    home.refresh()
                }
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
        activity.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        onStopOrDispose { activity.unregisterReceiver(receiver) }
    }

    val dark = settingsState.theme.isDark(isSystemInDarkTheme())
    val lightBars =
        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
    val firstDayOfWeek = WeekFields.of(LocalConfiguration.current.locales[0]).firstDayOfWeek
    SideEffect {
        home.updateFirstDayOfWeek(firstDayOfWeek)
        activity.window.insetsController?.setSystemBarsAppearance(if (dark) 0 else lightBars, lightBars)
    }

    var showSettings by rememberSaveable { mutableStateOf(false) }
    var revealHealthConnect by rememberSaveable { mutableStateOf(false) }
    val homeScroll = rememberScrollState()
    val connect = { permission.launch(READ_STEPS) }
    val closeSettings = {
        showSettings = false
        revealHealthConnect = false
        if (settingsState.file.message != null) settings.dismissFileMessage()
    }
    BackHandler(enabled = showSettings && !showPrivacy, onBack = closeSettings)
    CompositionLocalProvider(LocalPalette provides if (dark) DarkPalette else LightPalette) {
        if (showSettings) {
            SettingsScreen(
                state = settingsState,
                revealHealthConnect = revealHealthConnect,
                onHealthConnectRevealed = { revealHealthConnect = false },
                onBack = closeSettings,
                onConnect = connect,
                onRetry = { home.refresh(restart = true) },
                onAdjustGoal = settings::adjustGoal,
                onSetTheme = settings::saveTheme,
                onImport = { importDocument.launch(arrayOf("text/*")) },
                onExport = { exportDocument.launch(stepsCsvFileName(LocalDate.now())) },
                onDismissFileMessage = settings::dismissFileMessage,
            )
        } else {
            HomeScreen(
                homeState,
                homeScroll,
                onPreviousWeek = home::previousWeek,
                onNextWeek = home::nextWeek,
                onOpenSettings = { showSettings = true },
                onOpenHealthConnect = {
                    revealHealthConnect = true
                    showSettings = true
                },
            )
        }
        when {
            showPrivacy ->
                AppDialog(stringResource(R.string.privacy_title), { showPrivacy = false }) {
                    AppText(stringResource(R.string.health_connect_privacy_policy))
                    TextAction(stringResource(R.string.close)) { showPrivacy = false }
                }

            !showSettings ->
                settingsState.file.message?.let { message ->
                    AppDialog(stringResource(R.string.backup_result), settings::dismissFileMessage) {
                        AppText(message)
                        TextAction(stringResource(R.string.close), onClick = settings::dismissFileMessage)
                    }
                }
        }
    }
}

private fun Intent.opensPrivacyPolicy(packageName: String): Boolean =
    action == Intent.ACTION_VIEW_PERMISSION_USAGE &&
        component?.className == "$packageName.ViewHealthConnectPermissionUsageActivity"
