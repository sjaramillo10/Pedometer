package dev.sjaramillo.nemi.data

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.health.connect.AggregateRecordsGroupedByPeriodResponse
import android.health.connect.AggregateRecordsRequest
import android.health.connect.DeviceDataSource
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions.READ_STEPS
import android.health.connect.LocalTimeRangeFilter
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.core.os.asOutcomeReceiver
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import java.time.Period

/** Reads phone-recorded steps, including the current device's synthetic Health Connect origin. */
class PhoneStepsDataSource(
    private val context: Context,
) {
    internal fun access(): HealthConnectAccess =
        phoneStepsAccess(
            hasHealthConnect = context.getSystemService(HealthConnectManager::class.java) != null,
            extensionVersion = SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE),
            hasStepCounter = context.getSystemService(SensorManager::class.java)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null,
            hasPermission = context.checkSelfPermission(READ_STEPS) == PackageManager.PERMISSION_GRANTED,
        )

    suspend fun readDailySteps(
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Long> {
        val manager = checkNotNull(context.getSystemService(HealthConnectManager::class.java))
        val timeRange =
            LocalTimeRangeFilter
                .Builder()
                .setStartTime(start.atStartOfDay())
                .setEndTime(end.plusDays(1).atStartOfDay())
                .build()
        val request = AggregateRecordsRequest.Builder<Long>(timeRange).addAggregationType(STEPS_COUNT_TOTAL)
        val extensionVersion = SdkExtensions.getExtensionVersion(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
        val currentDeviceOrigin =
            if (supportsCurrentDeviceOrigin(Build.VERSION.SDK_INT, extensionVersion)) getCurrentDeviceOrigin(manager) else null
        for (packageName in phoneStepOriginPackages(currentDeviceOrigin)) {
            request.addDataOriginsFilter(DataOrigin.Builder().setPackageName(packageName).build())
        }

        val response =
            suspendCancellableCoroutine<List<AggregateRecordsGroupedByPeriodResponse<Long>>> { continuation ->
                manager.aggregateGroupByPeriod(request.build(), Period.ofDays(1), context.mainExecutor, continuation.asOutcomeReceiver())
            }

        return response
            .mapNotNull { bucket -> bucket.get(STEPS_COUNT_TOTAL)?.let { bucket.startTime.toLocalDate() to it } }
            .toMap()
    }

    // The caller checks API 37 or U Extension 22 before resolving this API.
    @android.annotation.SuppressLint("NewApi")
    private suspend fun getCurrentDeviceOrigin(manager: HealthConnectManager): String? =
        try {
            val source =
                suspendCancellableCoroutine<DeviceDataSource> { continuation ->
                    manager.getCurrentDeviceDataSource(context.mainExecutor, continuation.asOutcomeReceiver())
                }
            source.deviceDataOrigin.packageName
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            null
        }
}

internal fun phoneStepOriginPackages(currentDeviceOrigin: String?): Set<String> =
    buildSet {
        add("android")
        currentDeviceOrigin?.let(::add)
    }

internal fun supportsCurrentDeviceOrigin(
    sdkInt: Int,
    extensionVersion: Int,
): Boolean = sdkInt >= 37 || extensionVersion >= 22

internal fun phoneStepsAccess(
    hasHealthConnect: Boolean,
    extensionVersion: Int,
    hasStepCounter: Boolean,
    hasPermission: Boolean,
): HealthConnectAccess =
    when {
        !hasHealthConnect -> HealthConnectAccess.Unavailable
        extensionVersion < 20 -> HealthConnectAccess.UpdateRequired
        !hasStepCounter -> HealthConnectAccess.NoStepCounter
        !hasPermission -> HealthConnectAccess.PermissionRequired
        else -> HealthConnectAccess.Available
    }

internal enum class HealthConnectAccess {
    Available,
    PermissionRequired,
    Unavailable,
    UpdateRequired,
    NoStepCounter,
}
