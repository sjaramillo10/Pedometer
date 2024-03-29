/*
 * Copyright 2013 Thomas Hoffmann
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.sjaramillo.pedometer.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import dagger.hilt.android.AndroidEntryPoint
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.worker.StepsCounterWorker

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        findViewById<BottomNavigationView>(R.id.bottom_nav)
            .setupWithNavController(navController)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val appBarConfiguration = AppBarConfiguration(
            setOf(R.id.dest_home, R.id.dest_stats, R.id.dest_settings)
        )
        setupActionBarWithNavController(navController, appBarConfiguration)

        StepsCounterWorker.enqueuePeriodicWork(this)
    }

    override fun onResume() {
        super.onResume()
        if (checkActivityRecognitionPermission()) {
            checkBatteryOptimizationStatus()
        }
    }

    /**
     * Returns true if the permission is either granted or not required, false otherwise.
     */
    private fun checkActivityRecognitionPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q)
            return true // We are good, Activity Recognition permission is not required before Android Q

        if (isActivityRecognitionPermissionGranted())
            return true // We are good, Activity Recognition permission already granted

        if (shouldShowRequestPermissionRationale(Manifest.permission.ACTIVITY_RECOGNITION)) {
            showActivityRecognitionPermissionRationaleDialog()
        } else {
            requestActivityRecognitionPermission()
        }

        return false
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun isActivityRecognitionPermissionGranted() = PackageManager.PERMISSION_GRANTED ==
        checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION)

    /**
     * Create and show a rationale dialog which explains why is permission needed.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun showActivityRecognitionPermissionRationaleDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.activity_recognition_permission_rationale_title)
            setMessage(R.string.activity_recognition_permission_rationale_message)
            setPositiveButton(R.string.activity_recognition_permission_rationale_positive_button) { _, _ ->
                requestActivityRecognitionPermission()
            }
            setNegativeButton(R.string.activity_recognition_permission_rationale_negative_button) { _, _ ->
                showActivityRecognitionPermissionRequiredDialog()
            }
        }.run {
            create()
            show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun requestActivityRecognitionPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
            PERMISSION_REQUEST_ACTIVITY_RECOGNITION
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_ACTIVITY_RECOGNITION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                checkBatteryOptimizationStatus()
                return // We are good, Activity Recognition permission was granted
            } else {
                showActivityRecognitionPermissionRequiredDialog()
            }
        }
    }

    private fun showActivityRecognitionPermissionRequiredDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.activity_recognition_permission_required_title)
            setMessage(R.string.activity_recognition_permission_required_message)
            setPositiveButton(R.string.activity_recognition_permission_required_positive_button) { _, _ ->
                this@MainActivity.finish()
            }
            setCancelable(false)
        }.run {
            create()
            show()
        }
    }

    private fun checkBatteryOptimizationStatus() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
            return // Battery Optimization is only available since Android M

        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName).not()) {
            showIgnoreBatteryOptimizationDialog()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun showIgnoreBatteryOptimizationDialog() {
        AlertDialog.Builder(this).apply {
            setTitle(R.string.ignore_battery_optimization_title)
            setMessage(R.string.ignore_battery_optimization_message)
            setPositiveButton(R.string.ignore_battery_optimization_positive_button) { _, _ ->
                val intent = Intent()
                intent.action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                startActivity(intent)
            }
            setNegativeButton(R.string.ignore_battery_optimization_negative_button) { dialog, _ ->
                dialog.dismiss()
            }
        }.run {
            create()
            show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // This should work once faq is a destination
        // val navController = findNavController(R.id.nav_host_fragment)
        // return item.onNavDestinationSelected(navController) || super.onOptionsItemSelected(item)
        return when (item.itemId) {
            R.id.action_faq -> {
                val faqUri = Uri.parse("http://j4velin.de/faq/index.php?app=pm")
                val intent = Intent(Intent.ACTION_VIEW, faqUri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            }
            R.id.action_about -> {
                val tv = TextView(this).apply {
                    setPadding(10, 10, 10, 10)
                    setText(R.string.about_text_links)
                }
                try {
                    val versionName = packageManager.getPackageInfo(packageName, 0).versionName
                    tv.append(getString(R.string.about_app_version, versionName))
                } catch (exception: NameNotFoundException) {
                    // should not happen as the app is definitely installed when seeing the dialog
                    exception.printStackTrace()
                }
                tv.movementMethod = LinkMovementMethod.getInstance()

                AlertDialog.Builder(this).apply {
                    setTitle(R.string.about)
                    setView(tv)
                    setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                }.also { it.create().show() }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 1001
    }
}
