package dev.sjaramillo.pedometer.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.WindowInsets
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var healthConnectSyncCoordinator: HealthConnectSyncCoordinator

    private val requestHealthConnectPermission =
        registerForActivityResult(PermissionController.createRequestPermissionResultContract()) {
            refreshHealthConnect()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        findViewById<BottomNavigationView>(R.id.bottom_nav).setupWithNavController(navController)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        setupActionBarWithNavController(
            navController,
            AppBarConfiguration(setOf(R.id.dest_home, R.id.dest_stats, R.id.dest_settings)),
        )
        applyToolbarSystemBarInsets(toolbar)
    }

    override fun onResume() {
        super.onResume()
        refreshHealthConnect()
    }

    fun requestHealthConnectAccess() {
        requestHealthConnectPermission.launch(setOf(HealthConnectSyncCoordinator.READ_STEPS_PERMISSION))
    }

    private fun refreshHealthConnect() {
        lifecycleScope.launch { healthConnectSyncCoordinator.refresh() }
    }

    private fun applyToolbarSystemBarInsets(toolbar: Toolbar) {
        val initialHeight = toolbar.layoutParams.height
        val initialPaddingLeft = toolbar.paddingLeft
        val initialPaddingTop = toolbar.paddingTop
        val initialPaddingRight = toolbar.paddingRight
        val initialPaddingBottom = toolbar.paddingBottom

        toolbar.setOnApplyWindowInsetsListener { view, windowInsets ->
            val topInset = windowInsets.getInsets(WindowInsets.Type.systemBars()).top
            view.setPadding(
                initialPaddingLeft,
                initialPaddingTop + topInset,
                initialPaddingRight,
                initialPaddingBottom,
            )
            view.layoutParams.height = initialHeight + topInset
            view.requestLayout()
            windowInsets
        }
        toolbar.requestApplyInsets()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_faq -> {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("http://j4velin.de/faq/index.php?app=pm")))
                true
            }
            R.id.action_about -> {
                showAboutDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    private fun showAboutDialog() {
        val text =
            TextView(this).apply {
                setPadding(10, 10, 10, 10)
                setText(R.string.about_text_links)
                append(getString(R.string.about_app_version, packageManager.getPackageInfo(packageName, 0).versionName))
                movementMethod = LinkMovementMethod.getInstance()
            }
        AlertDialog
            .Builder(this)
            .setTitle(R.string.about)
            .setView(text)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }
}
