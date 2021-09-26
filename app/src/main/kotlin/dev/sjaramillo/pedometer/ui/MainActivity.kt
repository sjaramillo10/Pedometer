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

import android.app.AlertDialog
import android.app.Fragment
import android.content.Intent
import android.content.pm.PackageManager.NameNotFoundException
import android.net.Uri
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.SensorListener

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startService(Intent(this, SensorListener::class.java))
        if (savedInstanceState == null) {
            // Create new fragment and transaction
            val newFragment: Fragment = OverviewFragment()
            val transaction = fragmentManager.beginTransaction()

            // Replace whatever is in the fragment_container view with this
            // fragment, and add the transaction to the back stack
            transaction.replace(android.R.id.content, newFragment)

            // Commit the transaction
            transaction.commit()
        }

        // TODO Request Activity Recognition permission: https://www.raywenderlich.com/24859773-activity-recognition-api-tutorial-for-android-getting-started
    }

    override fun onBackPressed() {
        if (fragmentManager.backStackEntryCount > 0) {
            fragmentManager.popBackStackImmediate()
        } else {
            finish()
        }
    }

    fun optionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> fragmentManager.popBackStackImmediate()
            R.id.action_settings -> fragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment()).addToBackStack(null)
                .commit()
            R.id.action_faq -> {
                val faqUri = Uri.parse("http://j4velin.de/faq/index.php?app=pm")
                val intent = Intent(Intent.ACTION_VIEW, faqUri)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
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
            }
        }
        return true
    }
}
