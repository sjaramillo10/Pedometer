package dev.sjaramillo.nemi.ui

import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.health.connect.HealthPermissions.READ_STEPS
import android.net.Uri
import android.os.Bundle
import android.view.WindowInsetsController
import android.window.OnBackInvokedCallback
import android.window.OnBackInvokedDispatcher
import dev.sjaramillo.nemi.R
import dev.sjaramillo.nemi.data.stepsCsvFileName
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.WeekFields

internal class NemiRoute(
    private val activity: Activity,
    savedState: Bundle?,
    private val home: HomeViewModel,
    private val settings: SettingsViewModel,
) {
    private val renderScope = MainScope()
    private var renderJob: Job? = null
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
    private val settingsBack = OnBackInvokedCallback { closeSettings() }
    private var settingsBackRegistered = false

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
        render()
    }

    fun start() {
        renderJob =
            renderScope.launch {
                combine(home.uiState, settings.uiState) { homeState, settingsState -> homeState to settingsState }
                    .collect { (homeState, settingsState) -> render(homeState, settingsState) }
            }
        val filter =
            IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            }
        activity.registerReceiver(dateChanges, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    fun resume() {
        home.refresh()
    }

    fun stop() {
        renderJob?.cancel()
        renderJob = null
        activity.unregisterReceiver(dateChanges)
    }

    fun destroy() {
        renderScope.cancel()
        setSettingsBackRegistered(false)
        dialog?.setOnDismissListener(null)
        dialog?.dismiss()
    }

    fun onPermissionResult(requestCode: Int) {
        if (requestCode == READ_PERMISSION) home.refresh(restart = true)
    }

    fun onDocumentResult(
        requestCode: Int,
        resultCode: Int,
        uri: Uri?,
    ) {
        if (resultCode != Activity.RESULT_OK || uri == null) return
        when (requestCode) {
            IMPORT_DOCUMENT -> settings.importCsv(uri)
            EXPORT_DOCUMENT -> settings.exportCsv(uri)
        }
    }

    @Suppress("DEPRECATION")
    private fun launchDocumentPicker(
        action: String,
        mimeType: String,
        requestCode: Int,
        title: String? = null,
    ) {
        val intent =
            Intent(action).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = mimeType
                title?.let { putExtra(Intent.EXTRA_TITLE, it) }
            }
        activity.startActivityForResult(intent, requestCode)
    }

    private fun setSettingsBackRegistered(value: Boolean) {
        if (settingsBackRegistered == value) return
        val dispatcher = activity.onBackInvokedDispatcher
        if (value) {
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, settingsBack)
        } else {
            dispatcher.unregisterOnBackInvokedCallback(settingsBack)
        }
        settingsBackRegistered = value
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
            activity.window.decorView.windowInsetsController?.setSystemBarsAppearance(if (dark) 0 else lightBars, lightBars)
            palette = colors
        }
        setSettingsBackRegistered(showSettings)
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
                        onConnect = { activity.requestPermissions(arrayOf(READ_STEPS), READ_PERMISSION) },
                        onRetry = { home.refresh(restart = true) },
                        onAdjustGoal = settings::adjustGoal,
                        onSetTheme = settings::saveTheme,
                        onImport = { launchDocumentPicker(Intent.ACTION_OPEN_DOCUMENT, "text/*", IMPORT_DOCUMENT) },
                        onExport = {
                            launchDocumentPicker(
                                Intent.ACTION_CREATE_DOCUMENT,
                                "text/csv",
                                EXPORT_DOCUMENT,
                                stepsCsvFileName(LocalDate.now()),
                            )
                        },
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

private const val READ_PERMISSION = 1
private const val IMPORT_DOCUMENT = 2
private const val EXPORT_DOCUMENT = 3
