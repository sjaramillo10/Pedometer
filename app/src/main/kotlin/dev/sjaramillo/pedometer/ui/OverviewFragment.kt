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
import android.app.Fragment
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.*
import android.widget.TextView
import org.eazegraph.lib.models.PieModel
import org.eazegraph.lib.charts.PieChart
import dev.sjaramillo.pedometer.util.API26Wrapper
import dev.sjaramillo.pedometer.Database
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.SensorListener
import org.eazegraph.lib.charts.BarChart
import org.eazegraph.lib.models.BarModel
import dev.sjaramillo.pedometer.util.Logger
import dev.sjaramillo.pedometer.util.Util
import java.lang.Exception
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// TODO cleanup this file
// TODO Use AndroidX fragment
// TODO Use ViewBinding or not? Maybe go straight to Compose!
class OverviewFragment : Fragment(), SensorEventListener {

    private var stepsView: TextView? = null
    private var totalView: TextView? = null
    private var averageView: TextView? = null
    private var sliceGoal: PieModel? = null
    private var sliceCurrent: PieModel? = null
    private var pg: PieChart? = null
    private var todayOffset = 0
    private var total_start = 0
    private var goal = 0
    private var since_boot = 0
    private var total_days = 0
    private var showSteps = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        if (Build.VERSION.SDK_INT >= 26) {
            API26Wrapper.startForegroundService(
                activity,
                Intent(activity, SensorListener::class.java)
            )
        } else {
            activity.startService(Intent(activity, SensorListener::class.java))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v = inflater.inflate(R.layout.fragment_overview, null)
        stepsView = v.findViewById<View>(R.id.steps) as TextView
        totalView = v.findViewById<View>(R.id.total) as TextView
        averageView = v.findViewById<View>(R.id.average) as TextView
        pg = v.findViewById<View>(R.id.graph) as PieChart

        // slice for the steps taken today
        sliceCurrent = PieModel("", 0f, Color.parseColor("#99CC00"))
        pg!!.addPieSlice(sliceCurrent)

        // slice for the "missing" steps until reaching the goal
        sliceGoal =
            PieModel("", Fragment_Settings.DEFAULT_GOAL.toFloat(), Color.parseColor("#CC0000"))
        pg!!.addPieSlice(sliceGoal)
        pg!!.setOnClickListener {
            showSteps = !showSteps
            stepsDistanceChanged()
        }
        pg!!.isDrawValueInPie = false
        pg!!.isUsePieRotation = true
        pg!!.startAnimation()
        return v
    }

    override fun onResume() {
        super.onResume()
        activity.actionBar!!.setDisplayHomeAsUpEnabled(false)
        val db = Database.getInstance(activity)
        db.logState()
        // read today's offset
        todayOffset = db.getSteps(Util.today)
        val prefs = activity.getSharedPreferences("pedometer", Context.MODE_PRIVATE)
        goal = prefs.getInt("goal", Fragment_Settings.DEFAULT_GOAL)
        since_boot = db.currentSteps
        val pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot)

        // register a sensorlistener to live update the UI if a step is taken
        val sm = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (sensor == null) {
            AlertDialog.Builder(activity).setTitle(R.string.no_sensor)
                .setMessage(R.string.no_sensor_explain)
                .setOnDismissListener { activity.finish() }
                .setNeutralButton(android.R.string.ok) { dialogInterface, i -> dialogInterface.dismiss() }
                .create().show()
        } else {
            sm.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI, 0)
        }
        since_boot -= pauseDifference
        total_start = db.totalWithoutToday
        total_days = db.days
        db.close()
        stepsDistanceChanged()
    }

    /**
     * Call this method if the Fragment should update the "steps"/"km" text in
     * the pie graph as well as the pie and the bars graphs.
     */
    private fun stepsDistanceChanged() {
        if (showSteps) {
            (view!!.findViewById<View>(R.id.unit) as TextView).text =
                getString(R.string.steps)
        } else {
            var unit = activity.getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                .getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT)
            unit = if (unit == "cm") {
                "km"
            } else {
                "mi"
            }
            (view!!.findViewById<View>(R.id.unit) as TextView).text =
                unit
        }
        updatePie()
        updateBars()
    }

    override fun onPause() {
        super.onPause()
        try {
            val sm = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sm.unregisterListener(this)
        } catch (e: Exception) {
            Logger.log(e)
        }
        val db = Database.getInstance(activity)
        db.saveCurrentSteps(since_boot)
        db.close()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.main, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_split_count -> {
                Dialog_Split.getDialog(
                    activity,
                    total_start + Math.max(todayOffset + since_boot, 0)
                ).show()
                true
            }
            else -> (activity as MainActivity).optionsItemSelected(item)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // won't happen
    }

    override fun onSensorChanged(event: SensorEvent) {
        val msg = "UI - sensorChanged | todayOffset: $todayOffset since boot: ${event.values[0]}"
        Logger.log(msg)
        if (event.values[0] > Int.MAX_VALUE || event.values[0] == 0f) {
            return
        }
        if (todayOffset == Int.MIN_VALUE) {
            // no values for today
            // we dont know when the reboot was, so set todays steps to 0 by
            // initializing them with -STEPS_SINCE_BOOT
            todayOffset = (-event.values[0]).toInt()
            val db = Database.getInstance(activity)
            db.insertNewDay(
                Util.today, event.values[0]
                    .toInt()
            )
            db.close()
        }
        since_boot = event.values[0].toInt()
        updatePie()
    }

    /**
     * Updates the pie graph to show todays steps/distance as well as the
     * yesterday and total values. Should be called when switching from step
     * count to distance.
     */
    private fun updatePie() {
        Logger.log("UI - update steps: $since_boot")
        // todayOffset might still be Integer.MIN_VALUE on first start
        val steps_today = Math.max(todayOffset + since_boot, 0)
        sliceCurrent!!.value = steps_today.toFloat()
        if (goal - steps_today > 0) {
            // goal not reached yet
            if (pg!!.data.size == 1) {
                // can happen if the goal value was changed: old goal value was
                // reached but now there are some steps missing for the new goal
                pg!!.addPieSlice(sliceGoal)
            }
            sliceGoal!!.value = (goal - steps_today).toFloat()
        } else {
            // goal reached
            pg!!.clearChart()
            pg!!.addPieSlice(sliceCurrent)
        }
        pg!!.update()
        if (showSteps) {
            stepsView!!.text = formatter.format(steps_today.toLong())
            totalView!!.text =
                formatter.format((total_start + steps_today).toLong())
            averageView!!.text =
                formatter.format(((total_start + steps_today) / total_days).toLong())
        } else {
            // update only every 10 steps when displaying distance
            val prefs = activity.getSharedPreferences("pedometer", Context.MODE_PRIVATE)
            val stepsize = prefs.getFloat("stepsize_value", Fragment_Settings.DEFAULT_STEP_SIZE)
            var distance_today = steps_today * stepsize
            var distance_total = (total_start + steps_today) * stepsize
            if (prefs.getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT)
                == "cm"
            ) {
                distance_today /= 100000f
                distance_total /= 100000f
            } else {
                distance_today /= 5280f
                distance_total /= 5280f
            }
            stepsView!!.text = formatter.format(distance_today.toDouble())
            totalView!!.text = formatter.format(distance_total.toDouble())
            averageView!!.text =
                formatter.format((distance_total / total_days).toDouble())
        }
    }

    /**
     * Updates the bar graph to show the steps/distance of the last week. Should
     * be called when switching from step count to distance.
     */
    private fun updateBars() {
        val df = SimpleDateFormat("E", Locale.getDefault())
        val barChart = view!!.findViewById<View>(R.id.bargraph) as BarChart
        if (barChart.data.size > 0) barChart.clearChart()
        var steps: Int
        var distance: Float
        var stepsize = Fragment_Settings.DEFAULT_STEP_SIZE
        var stepsize_cm = true
        if (!showSteps) {
            // load some more settings if distance is needed
            val prefs = activity.getSharedPreferences("pedometer", Context.MODE_PRIVATE)
            stepsize = prefs.getFloat("stepsize_value", Fragment_Settings.DEFAULT_STEP_SIZE)
            stepsize_cm = (prefs.getString("stepsize_unit", Fragment_Settings.DEFAULT_STEP_UNIT)
                    == "cm")
        }
        barChart.isShowDecimal = !showSteps // show decimal in distance view only
        var bm: BarModel
        val db = Database.getInstance(activity)
        val last = db.getLastEntries(8)
        db.close()
        for (i in last.size - 1 downTo 1) {
            val current = last[i]
            steps = current.second
            if (steps > 0) {
                bm = BarModel(
                    df.format(Date(current.first)), 0f,
                    if (steps > goal) Color.parseColor("#99CC00") else Color.parseColor("#0099cc")
                )
                if (showSteps) {
                    bm.value = steps.toFloat()
                } else {
                    distance = steps * stepsize
                    distance /= if (stepsize_cm) {
                        100000f
                    } else {
                        5280f
                    }
                    distance = Math.round(distance * 1000) / 1000f // 3 decimals
                    bm.value = distance
                }
                barChart.addBar(bm)
            }
        }
        if (barChart.data.size > 0) {
            barChart.setOnClickListener { Dialog_Statistics.getDialog(activity, since_boot).show() }
            barChart.startAnimation()
        } else {
            barChart.visibility = View.GONE
        }
    }

    companion object {
        @JvmField
        val formatter = NumberFormat.getInstance(Locale.getDefault())
    }
}
