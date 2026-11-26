package dev.sjaramillo.nemi.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import dev.sjaramillo.nemi.NemiApplication

class MainActivity : Activity() {
    private lateinit var route: NemiRoute
    private lateinit var models: Models

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setDecorFitsSystemWindows(false)
        window.attributes =
            window.attributes.apply {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        window.isStatusBarContrastEnforced = false
        window.isNavigationBarContrastEnforced = false
        val nemi = application as NemiApplication
        models = lastNonConfigurationInstance as? Models ?: Models(nemi.createHomeViewModel(), nemi.createSettingsViewModel())
        route = NemiRoute(this, savedInstanceState, models.home, models.settings)
    }

    override fun onStart() {
        super.onStart()
        route.start()
    }

    override fun onResume() {
        super.onResume()
        route.resume()
    }

    override fun onStop() {
        route.stop()
        super.onStop()
    }

    override fun onDestroy() {
        route.destroy()
        if (!isChangingConfigurations) {
            models.home.close()
            models.settings.close()
        }
        super.onDestroy()
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onRetainNonConfigurationInstance(): Any = models

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        route.handleIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        route.saveState(outState)
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        route.onDocumentResult(requestCode, resultCode, data?.data)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        route.onPermissionResult(requestCode)
    }

    private class Models(val home: HomeViewModel, val settings: SettingsViewModel)
}
