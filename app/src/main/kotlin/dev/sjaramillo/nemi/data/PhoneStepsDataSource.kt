package dev.sjaramillo.nemi.data

import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.AggregateRecordsGroupedByPeriodResponse
import android.health.connect.AggregateRecordsRequest
import android.health.connect.DeviceDataSource
import android.health.connect.HealthConnectManager
import android.health.connect.HealthPermissions.READ_STEPS
import android.health.connect.LocalTimeRangeFilter
import android.health.connect.datatypes.DataOrigin
import android.health.connect.datatypes.StepsRecord.STEPS_COUNT_TOTAL
import androidx.core.os.asOutcomeReceiver
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import java.time.LocalDate
import java.time.Period

/** Reads phone-recorded steps, including the current device's synthetic Health Connect origin. */
@Inject
class PhoneStepsDataSource(
    private val context: Context,
) {
    internal fun access(): HealthConnectAccess =
        when {
            context.getSystemService(HealthConnectManager::class.java) == null -> HealthConnectAccess.Unavailable
            context.checkSelfPermission(READ_STEPS) == PackageManager.PERMISSION_GRANTED -> HealthConnectAccess.Available
            else -> HealthConnectAccess.PermissionRequired
        }

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
        for (packageName in phoneStepOriginPackages(getCurrentDeviceOrigin(manager))) {
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

internal enum class HealthConnectAccess {
    Available,
    PermissionRequired,
    Unavailable,
}
