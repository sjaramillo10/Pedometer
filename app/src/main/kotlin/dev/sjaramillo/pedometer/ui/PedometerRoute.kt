package dev.sjaramillo.pedometer.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sjaramillo.pedometer.data.READ_STEPS_PERMISSION

@Composable
internal fun PedometerRoute(
    activity: ComponentActivity,
    initialIntent: Intent,
    incomingIntent: Intent?,
) {
    val models = remember(activity) { ViewModelProvider(activity) }
    val home = remember(models) { models[HomeViewModel::class.java] }
    val state by home.uiState.collectAsStateWithLifecycle()
    val packageName = activity.packageName
    var showPrivacy by rememberSaveable { mutableStateOf(initialIntent.opensPrivacyPolicy(packageName)) }
    LaunchedEffect(incomingIntent) {
        incomingIntent?.let { showPrivacy = it.opensPrivacyPolicy(packageName) }
    }

    val permission =
        rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
            home.refresh()
        }
    val importDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(home::importCsv)
        }
    val exportDocument =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let(home::exportCsv)
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

    val dark = state.theme.isDark(isSystemInDarkTheme())
    val lightBars =
        WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
            WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
    SideEffect {
        activity.window.insetsController?.setSystemBarsAppearance(if (dark) 0 else lightBars, lightBars)
    }

    HomeScreen(
        state = state,
        dark = dark,
        showPrivacy = showPrivacy,
        onDismissPrivacy = { showPrivacy = false },
        onConnect = { permission.launch(setOf(READ_STEPS_PERMISSION)) },
        onImport = { importDocument.launch(arrayOf("text/*")) },
        onExport = { exportDocument.launch("Pedometer.csv") },
        onRefresh = home::refresh,
        onSaveGoal = home::saveGoal,
        onSetTheme = home::saveTheme,
        onDismissFileMessage = home::dismissFileMessage,
    )
}

private fun Intent.opensPrivacyPolicy(packageName: String): Boolean =
    action == Intent.ACTION_VIEW_PERMISSION_USAGE &&
        component?.className == "$packageName.ViewHealthConnectPermissionUsageActivity"
