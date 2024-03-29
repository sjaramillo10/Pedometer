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

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.WindowManager
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.RadioGroup
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.StepsRepository
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.FileReader
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max

// TODO cleanup this file
// TODO Use ViewBinding or not? Maybe go straight to Compose!
@AndroidEntryPoint
class SettingsFragment : PreferenceFragmentCompat(), Preference.OnPreferenceClickListener {

    @Inject
    lateinit var stepsRepository: StepsRepository

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        setHasOptionsMenu(true)

        val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)

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

        findPreference<Preference>("export")?.onPreferenceClickListener = this
        findPreference<Preference>("import")?.onPreferenceClickListener = this
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_main, menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        menu.findItem(R.id.action_split_count).isVisible = false
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
            "export" -> requestCsvUriToExportData()
            "import" -> requestCsvUriToImportData()
        }
        return false
    }

    private fun requestCsvUriToExportData() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "text/csv"
            // TODO create dynamic file name using current date, i.e. Pedometer-2021-09-28.csv
            putExtra(Intent.EXTRA_TITLE, "Pedometer.csv")
        }

        startActivityForResult(intent, REQUEST_CREATE_FILE)
    }

    private fun requestCsvUriToImportData() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // TODO Figure out which mime type to use, 'text/csv' does not work
        }

        startActivityForResult(intent, REQUEST_READ_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        if (requestCode == REQUEST_CREATE_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                resultData?.data?.also { uri -> writeDataToCsv(uri) }
            } else {
                // TODO Show error dialog
            }
        } else if (requestCode == REQUEST_READ_FILE) {
            if (resultCode == Activity.RESULT_OK) {
                resultData?.data?.also { uri -> readDataFromCsv(uri) }
            } else {
                // TODO Show error dialog
            }
        }
    }

    /**
     * Writes data containing past days' steps to a CSV file
     */
    private fun writeDataToCsv(uri: Uri) {
        val contentResolver = requireContext().applicationContext.contentResolver
        val dailySteps = stepsRepository.getAll()
        try {
            contentResolver.openFileDescriptor(uri, "w")?.use {
                FileOutputStream(it.fileDescriptor).use { stream ->
                    dailySteps.forEach { (day, steps) ->
                        val line = "$day,${max(steps, 0)}\n"
                        stream.write(line.toByteArray())
                    }
                }
            }
        } catch (e: FileNotFoundException) {
            // TODO Show pertinent error dialog
            e.printStackTrace()
            return
        } catch (e: IOException) {
            AlertDialog.Builder(context)
                .setMessage(getString(R.string.error_file, e.message))
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .create().show()
            e.printStackTrace()
            return
        }

        // TODO obtain created file name and use it in dialog message
        AlertDialog.Builder(activity)
            .setMessage(getString(R.string.data_saved, "f.absolutePath"))
            .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
            .create().show()
    }

    /**
     * Reads previously exported steps data from a CSV file
     *
     * Overwrites days for which there is already an entry in the database
     */
    private fun readDataFromCsv(uri: Uri) {
        val contentResolver = requireContext().applicationContext.contentResolver
        var ignored = 0
        var inserted = 0
        var overwritten = 0
        try {
            contentResolver.openFileDescriptor(uri, "r")?.use {
                FileReader(it.fileDescriptor).use { reader ->
                    reader.forEachLine { line ->
                        val data = line.split(",").map { item -> item.toLong() }
                        try {
                            if (stepsRepository.insertDayFromBackup(data[0], data[1])) {
                                inserted++
                            } else {
                                overwritten++
                            }
                        } catch (nfe: Exception) {
                            ignored++
                        }
                    }
                }
            }
        } catch (e: IOException) {
            AlertDialog.Builder(context)
                .setMessage(getString(R.string.error_file, e.message))
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .create().show()
            e.printStackTrace()
            return
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
    }

    companion object {
        const val REQUEST_CREATE_FILE = 1
        const val REQUEST_READ_FILE = 2

        const val DEFAULT_GOAL = 10000

        val DEFAULT_STEP_SIZE = if (Locale.getDefault() === Locale.US) 2.5f else 75f
        val DEFAULT_STEP_UNIT = if (Locale.getDefault() === Locale.US) "ft" else "cm"
    }
}
