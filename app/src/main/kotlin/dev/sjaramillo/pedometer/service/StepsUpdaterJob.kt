package dev.sjaramillo.pedometer.service

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import dev.sjaramillo.pedometer.data.PedometerDatabase
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.util.Logger.log
import java.time.Duration

/**
 * Attempts to obtain a reading from the device's step sensor to update the app's
 * step count.
 */
class StepsUpdaterJob: JobService(), SensorEventListener {

    override fun onStartJob(params: JobParameters?): Boolean {
        log("StepsUpdaterJob onJobStart()")

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
            return true
        }

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }

    override fun onSensorChanged(event: SensorEvent?) {
        log("onSensorChanged: sensorEvent: $event")
        event?.values?.firstOrNull()?.let { steps ->
            log("Step count: $steps ")
            val stepsRepository = StepsRepository(PedometerDatabase.getInstance(this))
            stepsRepository.updateStepsSinceBoot(steps.toLong())
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // nobody knows what happens here: step value might magically decrease
        // when this method is called...
        log(sensor?.name + " accuracy changed: " + accuracy)
    }

    companion object {
        private const val JOB_ID = 1001

        fun scheduleStepsUpdaterJob(context: Context) {
            val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, StepsUpdaterJob::class.java)
            val jobInfo = JobInfo.Builder(JOB_ID, componentName)
                .setPeriodic(Duration.ofMinutes(15).toMillis())
                .build()

            jobScheduler.schedule(jobInfo)
        }
    }
}
