package dev.sjaramillo.pedometer.data

import android.content.Context
import android.health.connect.DeviceDataSource
import android.health.connect.HealthConnectException
import android.health.connect.HealthConnectManager
import android.os.OutcomeReceiver
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Period
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/** Reads phone-recorded steps, including the current device's synthetic Health Connect origin. */
class PhoneStepsDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthConnectClient: HealthConnectClient,
) {
    suspend fun readDailySteps(
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Long> {
        val dataOrigins = phoneStepDataOrigins(getCurrentDeviceOrigin())

        val response =
            healthConnectClient.aggregateGroupByPeriod(
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

        return response.associate { result ->
            result.startTime.toLocalDate() to (result.result[StepsRecord.COUNT_TOTAL] ?: 0)
        }
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
