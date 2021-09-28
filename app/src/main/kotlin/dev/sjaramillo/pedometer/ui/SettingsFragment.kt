/*
 * Copyright 2014 Thomas Hoffmann
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
import android.app.Dialog
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import androidx.preference.Preference
import android.view.*
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.RadioGroup
import android.widget.Toast
import androidx.preference.CheckBoxPreference
import androidx.preference.PreferenceFragmentCompat
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.db.Database
import dev.sjaramillo.pedometer.service.SensorListener
import dev.sjaramillo.pedometer.util.API26Wrapper.launchNotificationSettings
import java.io.*
import java.util.*

// TODO cleanup this file
// TODO Use ViewBinding or not? Maybe go straight to Compose!
class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        setHasOptionsMenu(true)

        val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)

        findPreference<Preference>("import")?.onPreferenceClickListener = this
        findPreference<Preference>("export")?.onPreferenceClickListener = this

        if (Build.VERSION.SDK_INT >= 26) {
            findPreference<Preference>("notification")?.onPreferenceClickListener = this
        } else {
            findPreference<CheckBoxPreference>("notification")?.setOnPreferenceChangeListener { _, newValue ->
                prefs.edit().putBoolean("notification", (newValue as Boolean?)!!).apply()
                val manager: NotificationManager = requireContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (newValue) {
                    manager.notify(
                        SensorListener.NOTIFICATION_ID,
                        SensorListener.getNotification(requireContext())
                    )
                } else {
                    manager.cancel(SensorListener.NOTIFICATION_ID)
                }
                true
            }
        }

        val goal = findPreference<Preference>("goal")
        goal?.onPreferenceClickListener = this
        goal?.summary = getString(R.string.goal_summary, prefs.getInt("goal", DEFAULT_GOAL))

        val stepSize = findPreference<Preference>("step_size")
        stepSize?.onPreferenceClickListener = this
        stepSize?.summary = getString(
            R.string.step_size_summary,
            prefs.getFloat("step_size_value", DEFAULT_STEP_SIZE),
            prefs.getString("step_size_unit", DEFAULT_STEP_UNIT)
        )
    }

    override fun onResume() {
        super.onResume()
        requireActivity().actionBar?.setDisplayHomeAsUpEnabled(true)
        if (Build.VERSION.SDK_INT >= 26) { // notification settings might have changed
            requireContext().startForegroundService(Intent(context, SensorListener::class.java))
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_settings).isVisible = false
        menu.findItem(R.id.action_split_count).isVisible = false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return (activity as MainActivity).optionsItemSelected(item)
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val builder: AlertDialog.Builder
        val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
        when (preference.key) {
            "goal" -> {
                builder = AlertDialog.Builder(context)
                val np = NumberPicker(context)
                np.minValue = 1
                np.maxValue = 100000
                np.value = prefs.getInt("goal", 10000)
                builder.setView(np)
                builder.setTitle(R.string.set_goal)
                builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                    np.clearFocus()
                    prefs.edit().putInt("goal", np.value).apply()
                    preference.summary = getString(R.string.goal_summary, np.value)
                    dialog.dismiss()
                    requireContext().startService(
                        Intent(context, SensorListener::class.java)
                            .putExtra("updateNotificationState", true)
                    )
                }
                builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                val dialog: Dialog = builder.create()
                dialog.window
                    ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                dialog.show()
            }
            "step_size" -> {
                builder = AlertDialog.Builder(context)
                val view = LayoutInflater.from(context).inflate(R.layout.dialog_step_size, null)
                val unit = view.findViewById<RadioGroup>(R.id.unit)
                val value = view.findViewById<EditText>(R.id.value)
                val stepSizeUnit = prefs.getString("step_size_unit", DEFAULT_STEP_UNIT)
                unit.check(if ((stepSizeUnit == "cm")) R.id.cm else R.id.ft)
                value.setText(prefs.getFloat("step_size_value", DEFAULT_STEP_SIZE).toString())
                builder.setView(view)
                builder.setTitle(R.string.set_step_size)
                builder.setPositiveButton(android.R.string.ok) { dialog, _ ->
                    try {
                        prefs.edit()
                            .putFloat("step_size_value", value.text.toString().toFloat())
                            .putString(
                                "step_size_unit",
                                if (unit.checkedRadioButtonId == R.id.cm) "cm" else "ft"
                            )
                            .apply()

                        preference.summary = getString(
                            R.string.step_size_summary,
                            value.text.toString().toFloat(),
                            if (unit.checkedRadioButtonId == R.id.cm) "cm" else "ft"
                        )
                    } catch (nfe: NumberFormatException) {
                        nfe.printStackTrace()
                    }
                    dialog.dismiss()
                }
                builder.setNegativeButton(android.R.string.cancel) { dialog, _ -> dialog.dismiss() }
                builder.create().show()
            }
            "import", "export" -> if (hasWriteExternalPermission()) {
                if (preference.key == "import") {
                    importCsv()
                } else {
                    exportCsv()
                }
            } else if (Build.VERSION.SDK_INT >= 23) {
                // TODO Update code so that external storage permission is not required
                activity
                    ?.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 42)
            } else {
                Toast.makeText(
                    context, R.string.permission_external_storage,
                    Toast.LENGTH_SHORT
                ).show()
            }
            "notification" -> launchNotificationSettings(requireContext())
        }
        return false
    }

    private fun hasWriteExternalPermission(): Boolean {
        return requireContext().packageManager
            .checkPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                requireContext().packageName
            ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Creates the CSV file containing data about past days and the steps taken on them
     *
     *
     * Requires external storage to be writeable
     */
    private fun exportCsv() {
        if ((Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)) {
            val f = File(Environment.getExternalStorageDirectory(), "Pedometer.csv")
            if (f.exists()) {
                AlertDialog.Builder(context).setMessage(R.string.file_already_exists)
                    .setPositiveButton(android.R.string.ok) { dialog, _ ->
                        dialog.dismiss()
                        writeToFile(f)
                    }.setNegativeButton(
                        android.R.string.cancel
                    ) { dialog, _ -> dialog.dismiss() }
                    .create().show()
            } else {
                writeToFile(f)
            }
        } else {
            AlertDialog.Builder(context)
                .setMessage(R.string.error_external_storage_not_available)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .create().show()
        }
    }

    /**
     * Imports previously exported data from a csv file
     *
     * Requires external storage to be readable. Overwrites days for which there is already an entry in the database
     */
    private fun importCsv() {
        if ((Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED)) {
            val f = File(Environment.getExternalStorageDirectory(), "Pedometer.csv")
            if (!f.exists() || !f.canRead()) {
                AlertDialog.Builder(context)
                    .setMessage(getString(R.string.file_cant_read, f.absolutePath))
                    .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .create().show()
                return
            }
            val db = Database.getInstance(requireContext())
            var line: String?
            var data: Array<String?>
            var ignored = 0
            var inserted = 0
            var overwritten = 0
            val `in`: BufferedReader
            try {
                `in` = BufferedReader(FileReader(f))
                while ((`in`.readLine().also { line = it }) != null) {
                    data = line!!.split(";".toRegex()).toTypedArray()
                    try {
                        if (db.insertDayFromBackup(data[0]!!.toLong(), (data[1])!!.toInt())) {
                            inserted++
                        } else {
                            overwritten++
                        }
                    } catch (nfe: Exception) {
                        ignored++
                    }
                }
                `in`.close()
            } catch (e: IOException) {
                AlertDialog.Builder(context)
                    .setMessage(getString(R.string.error_file, e.message))
                    .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                    .create().show()
                e.printStackTrace()
                return
            } finally {
                db.close()
            }
            var message = getString(R.string.entries_imported, inserted + overwritten)
            if (overwritten > 0) message += "\n\n" + getString(
                R.string.entries_overwritten,
                overwritten
            )
            if (ignored > 0) message += "\n\n" + getString(R.string.entries_ignored, ignored)
            AlertDialog.Builder(context).setMessage(message)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .create().show()
        } else {
            AlertDialog.Builder(context)
                .setMessage(R.string.error_external_storage_not_available)
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .create().show()
        }
    }

    private fun writeToFile(f: File) {
        val out: BufferedWriter
        try {
            f.createNewFile()
            out = BufferedWriter(FileWriter(f))
        } catch (e: IOException) {
            AlertDialog.Builder(context)
                .setMessage(getString(R.string.error_file, e.message))
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .create().show()
            e.printStackTrace()
            return
        }
        val db = Database.getInstance(requireContext())
        val c = db.query(arrayOf("date", "steps"), "date > 0", null, null, null, "date", null)
        try {
            if (c.moveToFirst()) {
                while (!c.isAfterLast) {
                    out.append(c.getString(0)).append(";")
                        .append(c.getInt(1).coerceAtLeast(0).toString()).append("\n")
                    c.moveToNext()
                }
            }
            out.flush()
            out.close()
        } catch (e: IOException) {
            AlertDialog.Builder(activity)
                .setMessage(getString(R.string.error_file, e.message))
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .create().show()
            e.printStackTrace()
            return
        } finally {
            c.close()
            db.close()
        }
        AlertDialog.Builder(activity)
            .setMessage(getString(R.string.data_saved, f.absolutePath))
            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    companion object {
        const val DEFAULT_GOAL = 10000
        val DEFAULT_STEP_SIZE = if (Locale.getDefault() === Locale.US) 2.5f else 75f
        val DEFAULT_STEP_UNIT = if (Locale.getDefault() === Locale.US) "ft" else "cm"
    }
}
