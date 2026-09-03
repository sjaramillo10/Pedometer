package dev.sjaramillo.pedometer.ui

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.RadioGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import dagger.hilt.android.AndroidEntryPoint
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.data.decodeStepsCsv
import dev.sjaramillo.pedometer.data.encodeStepsCsv
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment :
    PreferenceFragmentCompat(),
    Preference.OnPreferenceClickListener {
    @Inject
    lateinit var healthConnectSyncCoordinator: HealthConnectSyncCoordinator

    @Inject
    lateinit var stepsRepository: StepsRepository

    private val createCsvDocument =
        registerForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
            uri?.let(::exportCsv)
        }

    private val openCsvDocument =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            uri?.let(::importCsv)
        }

    override fun onCreatePreferences(
        savedInstanceState: Bundle?,
        rootKey: String?,
    ) {
        setPreferencesFromResource(R.xml.settings, rootKey)

        val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
        findPreference<Preference>("goal")?.apply {
            onPreferenceClickListener = this@SettingsFragment
            summary = getString(R.string.goal_summary, prefs.getInt("goal", DEFAULT_GOAL))
        }
        findPreference<Preference>("step_size")?.apply {
            onPreferenceClickListener = this@SettingsFragment
            summary =
                getString(
                    R.string.step_size_summary,
                    prefs.getFloat("step_size_value", DEFAULT_STEP_SIZE),
                    prefs.getString("step_size_unit", DEFAULT_STEP_UNIT),
                )
        }
        findPreference<Preference>("export")?.onPreferenceClickListener = this
        findPreference<Preference>("import")?.onPreferenceClickListener = this
    }

    override fun onPreferenceClick(preference: Preference): Boolean {
        val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
        when (preference.key) {
            "goal" -> showGoalDialog(preference, prefs)
            "step_size" -> showStepSizeDialog(preference, prefs)
            "export" -> createCsvDocument.launch("Pedometer.csv")
            "import" -> openCsvDocument.launch(arrayOf("text/*"))
        }
        return true
    }

    private fun exportCsv(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val csv = encodeStepsCsv(stepsRepository.getAll())
                withContext(Dispatchers.IO) {
                    requireContext().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                        it.write(csv)
                    } ?: throw IOException("Unable to open export destination")
                }
            }.onSuccess {
                showMessage(R.string.data_exported)
            }.onFailure { error ->
                showMessage(R.string.error_file, error.message)
            }
        }
    }

    private fun importCsv(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                val importedSteps =
                    withContext(Dispatchers.IO) {
                        requireContext().contentResolver.openInputStream(uri)?.bufferedReader()?.use {
                            decodeStepsCsv(it.readText())
                        } ?: throw IOException("Unable to open import source")
                    }
                stepsRepository.importDailySteps(importedSteps.dailySteps)
                healthConnectSyncCoordinator.refresh()
                importedSteps
            }.onSuccess { importedSteps ->
                showMessage(
                    R.string.entries_imported,
                    importedSteps.dailySteps.size,
                    importedSteps.ignoredRows,
                )
            }.onFailure { error ->
                showMessage(R.string.error_file, error.message)
            }
        }
    }

    private fun showMessage(
        stringRes: Int,
        vararg formatArgs: Any?,
    ) {
        AlertDialog
            .Builder(requireContext())
            .setMessage(getString(stringRes, *formatArgs))
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showGoalDialog(
        preference: Preference,
        prefs: android.content.SharedPreferences,
    ) {
        val picker =
            NumberPicker(context).apply {
                minValue = 1
                maxValue = 100000
                value = prefs.getInt("goal", DEFAULT_GOAL)
            }
        AlertDialog
            .Builder(context)
            .setView(picker)
            .setTitle(R.string.set_goal)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                picker.clearFocus()
                prefs.edit().putInt("goal", picker.value).apply()
                preference.summary = getString(R.string.goal_summary, picker.value)
            }.setNegativeButton(android.R.string.cancel, null)
            .create()
            .also { dialog: Dialog ->
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
                dialog.show()
            }
    }

    private fun showStepSizeDialog(
        preference: Preference,
        prefs: android.content.SharedPreferences,
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_step_size, null)
        val unit = view.findViewById<RadioGroup>(R.id.unit)
        val value = view.findViewById<EditText>(R.id.value)
        val stepSizeUnit = prefs.getString("step_size_unit", DEFAULT_STEP_UNIT)
        unit.check(if (stepSizeUnit == "cm") R.id.cm else R.id.ft)
        value.setText(prefs.getFloat("step_size_value", DEFAULT_STEP_SIZE).toString())

        AlertDialog
            .Builder(context)
            .setView(view)
            .setTitle(R.string.set_step_size)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                value.text.toString().toFloatOrNull()?.let { stepSize ->
                    val stepSizeUnitValue = if (unit.checkedRadioButtonId == R.id.cm) "cm" else "ft"
                    prefs
                        .edit()
                        .putFloat("step_size_value", stepSize)
                        .putString("step_size_unit", stepSizeUnitValue)
                        .apply()
                    preference.summary = getString(R.string.step_size_summary, stepSize, stepSizeUnitValue)
                }
            }.setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    companion object {
        const val DEFAULT_GOAL = 10000

        val DEFAULT_STEP_SIZE = if (Locale.getDefault() === Locale.US) 2.5f else 75f
        val DEFAULT_STEP_UNIT = if (Locale.getDefault() === Locale.US) "ft" else "cm"
    }
}
