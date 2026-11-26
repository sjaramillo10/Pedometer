package dev.sjaramillo.nemi.ui

import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.health.connect.HealthPermissions.READ_STEPS
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dev.sjaramillo.nemi.R
import dev.sjaramillo.nemi.data.stepsCsvFileName
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields

internal class NemiRoute(
    private val activity: ComponentActivity,
    savedState: Bundle?,
) : DefaultLifecycleObserver {
    private val models = ViewModelProvider(activity)
    private val home = models[HomeViewModel::class.java]
    private val settings = models[SettingsViewModel::class.java]
    private var showSettings = savedState?.getBoolean("settings") ?: false
    private var showPrivacy = savedState?.getBoolean("privacy") ?: opensPrivacyPolicy(activity.intent)
    private var revealHealthConnect = savedState?.getBoolean("revealHealthConnect") ?: false
    private var homeScroll = savedState?.getInt("homeScroll") ?: 0
    private var settingsScroll = savedState?.getInt("settingsScroll") ?: 0
    private var homeScreen: HomeScreen? = null
    private var settingsScreen: SettingsScreen? = null
    private var palette: Palette? = null
    private var dialog: Dialog? = null
    private var dialogContent: Pair<Int, String>? = null
    private val settingsBack =
        object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() = closeSettings()
        }

    private val permission =
        activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            home.refresh(restart = true)
        }
    private val importDocument =
        activity.registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(settings::importCsv)
        }
    private val exportDocument =
        activity.registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let(settings::exportCsv)
        }
    private val dateChanges =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                home.refresh()
            }
        }

    init {
        home.updateFirstDayOfWeek(WeekFields.of(activity.resources.configuration.locales[0]).firstDayOfWeek)
        activity.onBackPressedDispatcher.addCallback(activity, settingsBack)
        activity.lifecycle.addObserver(this)
        render()
        activity.lifecycleScope.launch {
            activity.repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(home.uiState, settings.uiState) { homeState, settingsState -> homeState to settingsState }
                    .collect { (homeState, settingsState) -> render(homeState, settingsState) }
            }
        }
    }

    override fun onStart(owner: LifecycleOwner) {
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
        activity.registerReceiver(dateChanges, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    override fun onResume(owner: LifecycleOwner) {
        home.refresh()
    }

    override fun onStop(owner: LifecycleOwner) {
        activity.unregisterReceiver(dateChanges)
    }

    override fun onDestroy(owner: LifecycleOwner) {
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
    }

    fun handleIntent(intent: Intent) {
        showPrivacy = opensPrivacyPolicy(intent)
        render()
    }

    fun saveState(outState: Bundle) {
        rememberScroll()
        outState.putBoolean("settings", showSettings)
        outState.putBoolean("privacy", showPrivacy)
        outState.putBoolean("revealHealthConnect", revealHealthConnect)
        outState.putInt("homeScroll", homeScroll)
        outState.putInt("settingsScroll", settingsScroll)
    }

    private fun opensPrivacyPolicy(intent: Intent): Boolean =
        intent.action == Intent.ACTION_VIEW_PERMISSION_USAGE &&
            intent.component?.className == "${activity.packageName}.ViewHealthConnectPermissionUsageActivity"

    private fun openSettings(revealHealth: Boolean) {
        showSettings = true
        revealHealthConnect = revealHealth
        render()
    }

    private fun closeSettings() {
        showSettings = false
        revealHealthConnect = false
        settings.dismissFileMessage()
        render()
        settingsScroll = 0
    }

    private fun rememberScroll() {
        homeScreen?.let { homeScroll = it.scroll.scrollY }
        settingsScreen?.let { settingsScroll = it.scroll.scrollY }
    }

    private fun render(
        homeState: HomeUiState = home.uiState.value,
        settingsState: SettingsUiState = settings.uiState.value,
    ) {
        val systemDark = activity.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        val dark = settingsState.theme.isDark(systemDark)
        val colors = if (dark) DarkPalette else LightPalette
        val themeChanged = palette != colors
        if (themeChanged) {
            val lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            activity.window.insetsController?.setSystemBarsAppearance(if (dark) 0 else lightBars, lightBars)
            palette = colors
        }
        settingsBack.isEnabled = showSettings
        if (showSettings) {
            var screen = settingsScreen
            if (screen == null || themeChanged) {
                rememberScroll()
                homeScreen = null
                screen =
                    SettingsScreen(
                        activity,
                        colors,
                        ::closeSettings,
                        onConnect = { permission.launch(READ_STEPS) },
                        onRetry = { home.refresh(restart = true) },
                        onAdjustGoal = settings::adjustGoal,
                        onSetTheme = settings::saveTheme,
                        onImport = { importDocument.launch(arrayOf("text/*")) },
                        onExport = { exportDocument.launch(stepsCsvFileName(LocalDate.now())) },
                        onDismissFileMessage = settings::dismissFileMessage,
                    )
                settingsScreen = screen
                screen.bind(settingsState)
                activity.setContentView(screen.root)
                screen.restoreScroll(settingsScroll)
            } else {
                screen.bind(settingsState)
            }
            if (revealHealthConnect) {
                revealHealthConnect = false
                screen.revealHealthConnect()
            }
        } else {
            var screen = homeScreen
            if (screen == null || themeChanged) {
                rememberScroll()
                settingsScreen = null
                screen = HomeScreen(activity, colors, ::openSettings, home::previousWeek, home::nextWeek)
                homeScreen = screen
                screen.bind(homeState)
                activity.setContentView(screen.root)
                screen.restoreScroll(homeScroll)
            } else {
                screen.bind(homeState)
            }
        }
        updateDialog(settingsState, colors, themeChanged)
    }

    private fun updateDialog(
        settingsState: SettingsUiState,
        colors: Palette,
        themeChanged: Boolean,
    ) {
        val content =
            when {
                showPrivacy -> R.string.privacy_title to activity.getString(R.string.health_connect_privacy_policy)
                !showSettings -> settingsState.file.message?.let { R.string.backup_result to it }
                else -> null
            }
        if (content == dialogContent && !themeChanged) return
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
        dialogContent = content
        dialog =
            content?.let { (title, message) ->
                showAppDialog(activity, colors, title, message) {
                    if (title == R.string.privacy_title) showPrivacy = false else settings.dismissFileMessage()
                    render()
                }
            }
    }
}
