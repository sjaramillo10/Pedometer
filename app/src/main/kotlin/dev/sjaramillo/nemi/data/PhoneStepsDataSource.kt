package dev.sjaramillo.nemi.data

import android.content.Context
import android.health.connect.DeviceDataSource
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.os.OutcomeReceiver
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dev.zacsweers.metro.Inject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Reads phone-recorded steps, including the current device's synthetic Health Connect origin. */
@Inject
class PhoneStepsDataSource(
    private val context: Context,
    private val healthConnectClient: () -> HealthConnectClient,
) {
    internal suspend fun access(): HealthConnectAccess {
        if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
            return HealthConnectAccess.Unavailable
        }
        return if (READ_STEPS_PERMISSION in healthConnectClient().permissionController.getGrantedPermissions()) {
            HealthConnectAccess.Available
        } else {
            HealthConnectAccess.PermissionRequired
        }
    }

    suspend fun readDailySteps(
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Long> {
        val dataOrigins = phoneStepDataOrigins(getCurrentDeviceOrigin())

        val response =
            healthConnectClient().aggregateGroupByPeriod(
                AggregateGroupByPeriodRequest(
                    metrics = setOf(StepsRecord.COUNT_TOTAL),
                    timeRangeFilter =
                        TimeRangeFilter.between(
                            LocalDateTime.of(start, java.time.LocalTime.MIN),
                            LocalDateTime.of(end.plusDays(1), java.time.LocalTime.MIN),
                        ),
                    timeRangeSlicer = Period.ofDays(1),
                    dataOriginFilter = dataOrigins,
                ),
            )

        return response
            .mapNotNull { bucket ->
                bucket.result[StepsRecord.COUNT_TOTAL]?.let { bucket.startTime.toLocalDate() to it }
            }.toMap()
    }

    private suspend fun getCurrentDeviceOrigin(): String? =
        suspendCoroutine { continuation ->
            val healthConnectManager = context.getSystemService(HealthConnectManager::class.java)
            if (healthConnectManager == null) {
                continuation.resume(null)
                return@suspendCoroutine
            }

            runCatching {
                healthConnectManager.getCurrentDeviceDataSource(
                    context.mainExecutor,
                    object : OutcomeReceiver<DeviceDataSource, HealthConnectException> {
                        override fun onResult(result: DeviceDataSource) {
                            continuation.resume(result.deviceDataOrigin.packageName)
                        }

                        override fun onError(error: HealthConnectException) {
                            continuation.resume(null)
                        }
                    },
                )
            }.onFailure {
                continuation.resume(null)
            }
        }
}

internal fun phoneStepDataOrigins(currentDeviceOrigin: String?): Set<DataOrigin> =
    buildSet {
        add(DataOrigin(LEGACY_ANDROID_DATA_ORIGIN))
        currentDeviceOrigin?.let { add(DataOrigin(it)) }
    }

private const val LEGACY_ANDROID_DATA_ORIGIN = "android"

internal val READ_STEPS_PERMISSION = HealthPermission.getReadPermission(StepsRecord::class)

internal enum class HealthConnectAccess {
    Available,
    PermissionRequired,
    Unavailable,
}
