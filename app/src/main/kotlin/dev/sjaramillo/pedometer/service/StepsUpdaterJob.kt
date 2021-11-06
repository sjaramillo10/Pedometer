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
class StepsUpdaterJob : JobService(), SensorEventListener {

    private lateinit var params: JobParameters

    override fun onStartJob(params: JobParameters?): Boolean {
        log("StepsUpdaterJob onJobStart()")

        this.params = params ?: return false

        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)?.let { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
            return true
        }

        return false
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        unregisterSensorListener()
        return false
    }

    override fun onSensorChanged(event: SensorEvent?) {
        log("onSensorChanged: sensorEvent: $event")
        event?.values?.firstOrNull()?.let { steps ->
            log("Step count: $steps ")
            val stepsRepository = StepsRepository(PedometerDatabase.getInstance(this))
            stepsRepository.updateStepsSinceBoot(steps.toLong())
        }

        unregisterSensorListener()

        // Mark job as finished to avoid wasting resources
        jobFinished(params, false)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // nobody knows what happens here: step value might magically decrease
        // when this method is called...
        log(sensor?.name + " accuracy changed: " + accuracy)
    }

    private fun unregisterSensorListener() {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(this)
    }

    companion object {
        private const val JOB_ID = 1001

        /**
         * Schedules the StepsUpdaterJob to run at most every 15 minutes.
         */
        fun scheduleStepsUpdaterJob(context: Context) {
            val jobScheduler =
                context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val componentName = ComponentName(context, StepsUpdaterJob::class.java)
            val jobInfo = JobInfo.Builder(JOB_ID, componentName)
                .setPeriodic(Duration.ofMinutes(15).toMillis())
                .setPersisted(true)
                .build()

            jobScheduler.schedule(jobInfo)
        }
    }
}
