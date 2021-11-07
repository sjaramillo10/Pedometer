package dev.sjaramillo.pedometer.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.*
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.PedometerDatabase
import dev.sjaramillo.pedometer.data.StepsRepository
import logcat.logcat
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// TODO Inject repository
/**
 * Attempts to obtain a reading from the device's step sensor to update the app's
 * step count.
 */
class StepsCounterWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private lateinit var stepsRepository: StepsRepository

    override suspend fun doWork(): Result {

        stepsRepository = StepsRepository(PedometerDatabase.getInstance(applicationContext))

        setForeground(getForegroundInfo())

        val sensorManager = applicationContext
            .getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val stepCounterSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        return if (stepCounterSensor != null) {
            val success = getStepsCount(sensorManager, stepCounterSensor)
            if (success) Result.success() else Result.retry()
        } else {
            Result.failure()
        }
    }

    private suspend fun getStepsCount(
        sensorManager: SensorManager,
        stepCounterSensor: Sensor
    ) = suspendCoroutine<Boolean> { continuation ->
        val listener = object : SensorEventListener {

            override fun onSensorChanged(event: SensorEvent?) {
                // Make sure to remove listener to avoid wasting resources
                sensorManager.unregisterListener(this)

                event?.values?.firstOrNull()?.let { steps ->
                    val stepsToday = stepsRepository.updateStepsSinceBoot(steps.toLong())
                    logcat { "Step count: $steps, steps today: $stepsToday" }
                    continuation.resume(true)
                    return
                }
                continuation.resume(false)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // nobody knows what happens here: step value might magically decrease
                // when this method is called...
                logcat { sensor?.name + " accuracy changed: " + accuracy }
            }
        }

        sensorManager.registerListener(
            listener,
            stepCounterSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    /*
     * Creates an instance of ForegroundInfo required to run this Worker as expedited.
     */
    private fun getForegroundInfo(): ForegroundInfo {
        val title = applicationContext.getString(R.string.notification_title)
        val content = applicationContext.getString(R.string.notification_content)

        // Create a Notification channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        }

        val notification = NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setContentTitle(title)
            .setTicker(content)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID_UPDATING_STEPS, notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val channelName = applicationContext.getString(R.string.notification_channel_name)
        val description = applicationContext.getString(R.string.notification_channel_description)
        val importance = NotificationManager.IMPORTANCE_DEFAULT

        val mChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance)
        mChannel.description = description

        // Register the channel with the system
        val notificationManager = applicationContext
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(mChannel)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "Notification"
        private const val NOTIFICATION_ID_UPDATING_STEPS = 1

        fun enqueuePeriodicWork(context: Context) {
            val stepsCounterWorker =
                PeriodicWorkRequestBuilder<StepsCounterWorker>(15, TimeUnit.MINUTES)
                    //.setExpedited(OutOfQuotaPolicy.DROP_WORK_REQUEST)
                    .addTag("stepsWork")
                    .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "periodicStepCounterWorker",
                ExistingPeriodicWorkPolicy.REPLACE,
                stepsCounterWorker
            )
        }
    }
}
