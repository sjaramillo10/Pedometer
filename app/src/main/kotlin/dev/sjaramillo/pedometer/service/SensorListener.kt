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
package dev.sjaramillo.pedometer.service

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import dev.sjaramillo.pedometer.db.Database
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.util.Logger.log
import dev.sjaramillo.pedometer.util.Util.today
import dev.sjaramillo.pedometer.util.Util.tomorrow
import dev.sjaramillo.pedometer.util.API26Wrapper.getNotificationBuilder
import dev.sjaramillo.pedometer.receiver.ShutdownReceiver
import dev.sjaramillo.pedometer.ui.MainActivity
import java.lang.Exception
import java.text.NumberFormat
import java.util.*
import kotlin.math.min

/**
 * Background service which keeps the step-sensor listener alive to always get
 * the number of steps since boot.
 *
 *
 * This service won't be needed any more if there is a way to read the
 * step-value without waiting for a sensor event
 */
class SensorListener : Service(), SensorEventListener {

    private val shutdownReceiver: BroadcastReceiver = ShutdownReceiver()

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // nobody knows what happens here: step value might magically decrease
        // when this method is called...
        log(sensor.name + " accuracy changed: " + accuracy)
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (event.values[0] > Int.MAX_VALUE) {
            log("probably not a real value: " + event.values[0])
        } else {
            steps = event.values[0].toInt()
            updateIfNecessary()
        }
    }

    /**
     * @return true, if notification was updated
     */
    private fun updateIfNecessary(): Boolean {
        return if (steps > lastSaveSteps + SAVE_OFFSET_STEPS ||
            steps > 0 && System.currentTimeMillis() > lastSaveTime + SAVE_OFFSET_TIME
        ) {
            log(
                "saving steps: steps=" + steps + " lastSave=" + lastSaveSteps +
                        " lastSaveTime=" + Date(lastSaveTime)
            )
            val db = Database.getInstance(this)
            if (db.getSteps(today) == Int.MIN_VALUE) {
                db.insertNewDay(today, steps)
            }
            db.saveCurrentSteps(steps)
            db.close()
            lastSaveSteps = steps
            lastSaveTime = System.currentTimeMillis()
            showNotification() // update notification
            true
        } else {
            false
        }
    }

    private fun showNotification() {
        if (Build.VERSION.SDK_INT >= 26) {
            startForeground(NOTIFICATION_ID, getNotification(this))
        } else if (getSharedPreferences("pedometer", MODE_PRIVATE)
                .getBoolean("notification", true)
        ) {
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager)
                .notify(NOTIFICATION_ID, getNotification(this))
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        reRegisterSensor()
        registerBroadcastReceiver()
        if (!updateIfNecessary()) {
            showNotification()
        }

        // restart service every hour to save the current step count
        val nextUpdate = min(tomorrow, System.currentTimeMillis() + AlarmManager.INTERVAL_HOUR)
        log("next update: " + Date(nextUpdate).toLocaleString())
        val alarmManager = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent
            .getService(
                applicationContext, 2, Intent(this, SensorListener::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT
            )
        if (Build.VERSION.SDK_INT >= 23) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC, nextUpdate, pendingIntent)
        } else {
            alarmManager[AlarmManager.RTC, nextUpdate] = pendingIntent
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        log("SensorListener onCreate")
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        log("sensor service task removed")
        // Restart service in 500 ms
        (getSystemService(ALARM_SERVICE) as AlarmManager)[AlarmManager.RTC, System.currentTimeMillis() + 500] =
            PendingIntent
                .getService(this, 3, Intent(this, SensorListener::class.java), 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        log("SensorListener onDestroy")
        try {
            val sm = getSystemService(SENSOR_SERVICE) as SensorManager
            sm.unregisterListener(this)
            unregisterReceiver(shutdownReceiver)
        } catch (e: Exception) {
            log(e)
        }
    }

    private fun registerBroadcastReceiver() {
        log("register broadcast receiver")
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_SHUTDOWN)
        registerReceiver(shutdownReceiver, filter)
    }

    private fun reRegisterSensor() {
        log("re-register sensor listener")
        val sm = getSystemService(SENSOR_SERVICE) as SensorManager
        try {
            sm.unregisterListener(this)
        } catch (e: Exception) {
            log(e)
        }
        log("step sensors: " + sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size)
        if (sm.getSensorList(Sensor.TYPE_STEP_COUNTER).size < 1) return  // emulator
        log("default: " + sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER).name)

        // enable batching with delay of max 5 min
        val enabled = sm.registerListener(
            this, sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER),
            SensorManager.SENSOR_DELAY_NORMAL, (5 * MICROSECONDS_IN_ONE_MINUTE).toInt()
        )
        log("Steps SensorListener enabled: $enabled")
    }

    companion object {
        const val NOTIFICATION_ID = 1
        private const val MICROSECONDS_IN_ONE_MINUTE: Long = 60000000
        private const val SAVE_OFFSET_TIME = AlarmManager.INTERVAL_HOUR
        private const val SAVE_OFFSET_STEPS = 500
        private var steps = 0
        private var lastSaveSteps = 0
        private var lastSaveTime: Long = 0

        fun getNotification(context: Context): Notification {
            log("getNotification")
            val prefs = context.getSharedPreferences("pedometer", MODE_PRIVATE)
            val goal = prefs.getInt("goal", 10000)
            val db = Database.getInstance(context)
            var todayOffset = db.getSteps(today)
            if (steps == 0) steps = db.currentSteps // use saved value if we haven't anything better
            db.close()
            val notificationBuilder =
                if (Build.VERSION.SDK_INT >= 26) getNotificationBuilder(context) else Notification.Builder(
                    context
                )
            if (steps > 0) {
                if (todayOffset == Int.MIN_VALUE) todayOffset = -steps
                val format = NumberFormat.getInstance(Locale.getDefault())
                notificationBuilder.setProgress(goal, todayOffset + steps, false).setContentText(
                    if (todayOffset + steps >= goal) context.getString(
                        R.string.goal_reached_notification,
                        format.format((todayOffset + steps).toLong())
                    ) else context.getString(
                        R.string.notification_text,
                        format.format((goal - todayOffset - steps).toLong())
                    )
                ).setContentTitle(
                    format.format((todayOffset + steps).toLong()) + " " + context.getString(R.string.steps)
                )
            } else { // still no step value?
                notificationBuilder.setContentText(
                    context.getString(R.string.your_progress_will_be_shown_here_soon)
                )
                    .setContentTitle(context.getString(R.string.notification_title))
            }
            notificationBuilder.setPriority(Notification.PRIORITY_MIN).setShowWhen(false)
                .setContentIntent(
                    PendingIntent
                        .getActivity(
                            context, 0, Intent(context, MainActivity::class.java),
                            PendingIntent.FLAG_UPDATE_CURRENT
                        )
                )
                .setSmallIcon(R.drawable.ic_notification).setOngoing(true)
            return notificationBuilder.build()
        }
    }
}