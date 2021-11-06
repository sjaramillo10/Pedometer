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

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.PedometerDatabase
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.util.DateUtil
import dev.sjaramillo.pedometer.util.FormatUtil
import dev.sjaramillo.pedometer.util.Logger
import org.eazegraph.lib.charts.BarChart
import org.eazegraph.lib.charts.PieChart
import org.eazegraph.lib.models.BarModel
import org.eazegraph.lib.models.PieModel
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

// TODO cleanup this file
// TODO Use ViewBinding or not? Maybe go straight to Compose!
class OverviewFragment : Fragment(), SensorEventListener {

    private lateinit var stepsView: TextView
    private lateinit var totalView: TextView
    private lateinit var averageView: TextView
    private lateinit var sliceGoal: PieModel
    private lateinit var sliceCurrent: PieModel
    private lateinit var graph: PieChart
    private var stepsUntilToday: Long = 0
    private var goal = 0
    private var totalDays = 0
    private var showSteps = true

    private lateinit var stepsRepository: StepsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        stepsRepository = StepsRepository(PedometerDatabase.getInstance(requireContext()))
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_overview, container, false)
        stepsView = v.findViewById<View>(R.id.steps) as TextView
        totalView = v.findViewById<View>(R.id.total) as TextView
        averageView = v.findViewById<View>(R.id.average) as TextView
        graph = v.findViewById<View>(R.id.graph) as PieChart

        // slice for the steps taken today
        sliceCurrent = PieModel("", 0f, Color.parseColor("#99CC00"))
        graph.addPieSlice(sliceCurrent)

        // slice for the "missing" steps until reaching the goal
        sliceGoal =
            PieModel("", SettingsFragment.DEFAULT_GOAL.toFloat(), Color.parseColor("#CC0000"))
        graph.addPieSlice(sliceGoal)
        graph.setOnClickListener {
            showSteps = !showSteps
            stepsDistanceChanged()
        }
        graph.isDrawValueInPie = false
        graph.isUsePieRotation = true
        graph.startAnimation()
        return v
    }

    override fun onResume() {
        super.onResume()
        activity?.actionBar?.setDisplayHomeAsUpEnabled(false)

        val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
        goal = prefs.getInt("goal", SettingsFragment.DEFAULT_GOAL)

        // register a sensor listener to live update the UI if a step is taken
        val sm = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            AlertDialog.Builder(context).setTitle(R.string.no_sensor)
                .setMessage(R.string.no_sensor_explain)
                .setOnDismissListener { activity?.finish() }
                .setPositiveButton(android.R.string.ok) { dialog, _ -> dialog.dismiss() }
                .create().show()
        } else {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0)
        }
        stepsUntilToday = stepsRepository.getStepsUntilToday()
        totalDays = stepsRepository.getDays().toInt()
        stepsDistanceChanged()
    }

    /**
     * Call this method if the Fragment should update the "steps"/"km" text in
     * the pie graph as well as the pie and the bars graphs.
     */
    private fun stepsDistanceChanged() {
        if (showSteps) {
            requireView().findViewById<TextView>(R.id.unit).text = getString(R.string.steps)
        } else {
            var unit = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                .getString("step_size_unit", SettingsFragment.DEFAULT_STEP_UNIT)
            unit = if (unit == "cm") "km" else "mi"
            requireView().findViewById<TextView>(R.id.unit).text = unit
        }
        val stepsToday = stepsRepository.getStepsToday()
        updatePie(stepsToday)
        updateBars()
    }

    override fun onPause() {
        super.onPause()
        try {
            val sm = context?.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sm.unregisterListener(this)
        } catch (e: Exception) {
            Logger.log(e)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_split_count -> {
                val stepsToday = stepsRepository.getStepsToday()
                SplitDialog.getDialog(requireContext(), stepsUntilToday + stepsToday).show()
                true
            }
            else -> (activity as MainActivity).optionsItemSelected(item)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // won't happen
    }

    override fun onSensorChanged(event: SensorEvent) {
        val msg = "UI - sensorChanged | since boot: ${event.values[0]}"
        Logger.log(msg)
        if (event.values[0] > Int.MAX_VALUE || event.values[0] == 0f) return

        val steps = event.values[0].toLong()
        val stepsToday = stepsRepository.updateStepsSinceBoot(steps)
        updatePie(stepsToday)
    }

    /**
     * Updates the pie graph to show todays steps/distance as well as the
     * yesterday and total values. Should be called when switching from step
     * count to distance.
     */
    private fun updatePie(stepsToday: Long) {
        Logger.log("UI - update stepsToday: $stepsToday")
        sliceCurrent.value = stepsToday.toFloat()
        if (goal - stepsToday > 0) {
            // goal not reached yet
            if (graph.data.size == 1) {
                // can happen if the goal value was changed: old goal value was
                // reached but now there are some steps missing for the new goal
                graph.addPieSlice(sliceGoal)
            }
            sliceGoal.value = (goal - stepsToday).toFloat()
        } else {
            // goal reached
            graph.clearChart()
            graph.addPieSlice(sliceCurrent)
        }
        graph.update()

        val numberFormat = FormatUtil.numberFormat
        if (showSteps) {
            stepsView.text = numberFormat.format(stepsToday)
            totalView.text = numberFormat.format(stepsUntilToday + stepsToday)
            averageView.text = numberFormat.format((stepsUntilToday + stepsToday) / totalDays)
        } else {
            // update only every 10 steps when displaying distance
            val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
            val stepSize = prefs.getFloat("step_size_value", SettingsFragment.DEFAULT_STEP_SIZE)
            var distanceToday = stepsToday * stepSize
            var distanceTotal = (stepsUntilToday + stepsToday) * stepSize
            if (prefs.getString("step_size_unit", SettingsFragment.DEFAULT_STEP_UNIT) == "cm") {
                distanceToday /= 100000f
                distanceTotal /= 100000f
            } else {
                distanceToday /= 5280f
                distanceTotal /= 5280f
            }
            stepsView.text = numberFormat.format(distanceToday.toDouble())
            totalView.text = numberFormat.format(distanceTotal.toDouble())
            averageView.text = numberFormat.format((distanceTotal / totalDays).toDouble())
        }
    }

    /**
     * Updates the bar graph to show the steps/distance of the last week. Should
     * be called when switching from step count to distance.
     */
    private fun updateBars() {
        val dtf = DateTimeFormatter.ofPattern("E")
        val barChart = requireView().findViewById<BarChart>(R.id.bargraph)
        if (barChart.data.size > 0) barChart.clearChart()
        var steps: Int
        var distance: Float
        var stepsize = SettingsFragment.DEFAULT_STEP_SIZE
        var stepSizeCm = true
        if (!showSteps) {
            // load some more settings if distance is needed
            val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
            stepsize = prefs.getFloat("step_size_value", SettingsFragment.DEFAULT_STEP_SIZE)
            stepSizeCm =
                prefs.getString("step_size_unit", SettingsFragment.DEFAULT_STEP_UNIT) == "cm"
        }
        barChart.isShowDecimal = !showSteps // show decimal in distance view only
        var bm: BarModel
        val lastEntries = stepsRepository.getLastEntries(8)
        for (i in lastEntries.size - 1 downTo 1) {
            val current = lastEntries[i]
            steps = current.steps.toInt()
            if (steps > 0) {
                bm = BarModel(
                    dtf.format(DateUtil.dayToLocalDate(current.day)), 0f,
                    if (steps > goal) Color.parseColor("#99CC00") else Color.parseColor("#0099cc")
                )
                if (showSteps) {
                    bm.value = steps.toFloat()
                } else {
                    distance = steps * stepsize
                    distance /= if (stepSizeCm) 100000f else 5280f
                    distance = (distance * 1000).roundToLong() / 1000f // 3 decimals
                    bm.value = distance
                }
                barChart.addBar(bm)
            }
        }
        if (barChart.data.size > 0) {
            barChart.setOnClickListener {
                StatisticsDialog.getDialog(requireContext()).show()
            }
            barChart.startAnimation()
        } else {
            barChart.visibility = View.GONE
        }
    }
}
