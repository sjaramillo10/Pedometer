package dev.sjaramillo.pedometer.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.health.connect.client.PermissionController
import dagger.hilt.android.AndroidEntryPoint
import dev.sjaramillo.pedometer.data.READ_STEPS_PERMISSION
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.ui.stats.StatsViewModel
import dev.sjaramillo.pedometer.ui.stats.StatsViewModelFactory
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var stepsRepository: StepsRepository

    private val home: HomeViewModel by viewModels()
    private val stats: StatsViewModel by viewModels { StatsViewModelFactory(stepsRepository) }
    private var showPrivacy by mutableStateOf(false)
    private val dateChanges =
        object : BroadcastReceiver() {
            override fun onReceive(
                context: Context,
                intent: Intent,
            ) {
                home.refresh()
            }
        }
    private val requestPermission =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) {
            home.refresh()
        }
    private val importDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(home::importCsv)
        }
    private val exportDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let(home::exportCsv)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        showPrivacy = savedInstanceState?.getBoolean(PRIVACY_VISIBLE) ?: isPrivacyIntent(intent)
        val version = packageManager.getPackageInfo(packageName, 0).versionName.orEmpty()
        setContent {
            val dark = isSystemInDarkTheme()
            val lightBars =
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or
                    WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            SideEffect {
                window.insetsController?.setSystemBarsAppearance(if (dark) 0 else lightBars, lightBars)
            }
            PedometerScreen(
                home = home,
                stats = { stats },
                showPrivacy = showPrivacy,
                onDismissPrivacy = { showPrivacy = false },
                onConnect = { requestPermission.launch(setOf(READ_STEPS_PERMISSION)) },
                onImport = { importDocument.launch(arrayOf("text/*")) },
                onExport = { exportDocument.launch("Pedometer.csv") },
                version = version,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        showPrivacy = isPrivacyIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(PRIVACY_VISIBLE, showPrivacy)
    }

    override fun onStart() {
        super.onStart()
        registerReceiver(
            dateChanges,
            IntentFilter().apply {
                addAction(Intent.ACTION_DATE_CHANGED)
                addAction(Intent.ACTION_TIME_CHANGED)
                addAction(Intent.ACTION_TIMEZONE_CHANGED)
            },
            RECEIVER_NOT_EXPORTED,
        )
    }

    override fun onResume() {
        super.onResume()
        home.refresh()
    }

    override fun onStop() {
        unregisterReceiver(dateChanges)
        super.onStop()
    }

    private fun isPrivacyIntent(intent: Intent): Boolean =
        intent.action == Intent.ACTION_VIEW_PERMISSION_USAGE &&
            intent.component?.className == "$packageName.ViewHealthConnectPermissionUsageActivity"

    private companion object {
        const val PRIVACY_VISIBLE = "privacy_visible"
    }
}
