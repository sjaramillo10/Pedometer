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

import android.app.Dialog
import android.content.Context
import android.view.View
import android.widget.Button
import android.widget.TextView
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.util.FormatUtil
import java.text.DateFormat

// TODO Extend from Dialog class
internal object SplitDialog {

    private var isSplitActive = false

    fun getDialog(context: Context, totalSteps: Long): Dialog {
        // TODO Inject Database
        val prefs = context.getSharedPreferences("pedometer", Context.MODE_MULTI_PROCESS)
        val splitDate = prefs.getLong("split_date", -1)
        val splitSteps = prefs.getLong("split_steps", totalSteps)
        val stepSize = prefs.getFloat("step_size_value", SettingsFragment.DEFAULT_STEP_SIZE)
        var distance = (totalSteps - splitSteps) * stepSize
        val distanceUnit: String
        if (prefs.getString("step_size_unit", SettingsFragment.DEFAULT_STEP_UNIT) == "cm") {
            distance /= 100000f
            distanceUnit = context.getString(R.string.distance_unit_km)
        } else {
            distance /= 5280f
            distanceUnit = context.getString(R.string.distance_unit_mi)
        }
        val numberFormat = FormatUtil.numberFormat

        val dialog = Dialog(context)
        dialog.setTitle(R.string.split_count)
        dialog.setContentView(R.layout.dialog_split)
        dialog.findViewById<TextView>(R.id.steps).text =
            numberFormat.format((totalSteps - splitSteps).toLong())
        dialog.findViewById<TextView>(R.id.distanceunit).text = distanceUnit
        dialog.findViewById<TextView>(R.id.distance).text =
            numberFormat.format(distance.toDouble())
        dialog.findViewById<TextView>(R.id.date).text = context.getString(
            R.string.since,
            DateFormat.getDateTimeInstance().format(splitDate)
        )
        val started = dialog.findViewById<View>(R.id.started)
        val stopped = dialog.findViewById<View>(R.id.stopped)
        isSplitActive = splitDate > 0
        started.visibility = if (isSplitActive) View.VISIBLE else View.GONE
        stopped.visibility = if (isSplitActive) View.GONE else View.VISIBLE
        val startStopButton = dialog.findViewById<Button>(R.id.start)
        startStopButton.setText(if (isSplitActive) R.string.stop else R.string.start)
        startStopButton.setOnClickListener {
            if (!isSplitActive) {
                prefs.edit().putLong("split_date", System.currentTimeMillis())
                    .putLong("split_steps", totalSteps).apply()
                isSplitActive = true
                dialog.dismiss()
            } else {
                started.visibility = View.GONE
                stopped.visibility = View.VISIBLE
                prefs.edit().remove("split_date").remove("split_steps").apply()
                isSplitActive = false
            }
            startStopButton.setText(if (isSplitActive) R.string.stop else R.string.start)
        }
        dialog.findViewById<View>(R.id.close).setOnClickListener { dialog.dismiss() }
        return dialog
    }
}
