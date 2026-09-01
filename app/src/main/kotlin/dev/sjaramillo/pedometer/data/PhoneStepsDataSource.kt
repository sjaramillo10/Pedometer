package dev.sjaramillo.pedometer.data

import android.content.Context
import android.health.connect.HealthConnectManager
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

class PhoneStepsDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val healthConnectClient: HealthConnectClient,
) {
    suspend fun readDailySteps(
        start: LocalDate,
        end: LocalDate,
    ): Map<LocalDate, Long> {
        val dataOrigins = mutableSetOf(DataOrigin(LEGACY_ANDROID_DATA_ORIGIN))
        val currentDeviceOrigin = getCurrentDeviceOrigin()
        currentDeviceOrigin?.let { dataOrigins += DataOrigin(it) }

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

    private companion object {
        const val LEGACY_ANDROID_DATA_ORIGIN = "android"
    }

    private fun getCurrentDeviceOrigin(): String? =
        runCatching {
            val healthConnectManager = context.getSystemService(HealthConnectManager::class.java)
            val dataSource =
                HealthConnectManager::class
                    .java
                    .getMethod("getCurrentDeviceDataSource")
                    .invoke(healthConnectManager)
            val dataOrigin = dataSource.javaClass.getMethod("getDeviceDataOrigin").invoke(dataSource)
            dataOrigin.javaClass.getMethod("getPackageName").invoke(dataOrigin) as String
        }.getOrNull()
}
