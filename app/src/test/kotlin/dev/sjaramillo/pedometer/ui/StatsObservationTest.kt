package dev.sjaramillo.pedometer.ui

import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.StepSource
import dev.sjaramillo.pedometer.data.StoredDailySteps
import dev.sjaramillo.pedometer.data.resolveDailySteps
import dev.sjaramillo.pedometer.ui.stats.StatsData
import dev.sjaramillo.pedometer.ui.stats.observeStats
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsObservationTest {
    @Test
    fun `given resolved history when refresh and import commit then record and lifetime reflect effective totals`() =
        runBlocking {
            // Given
            val backup = StoredDailySteps(20, 12_000, StepSource.BACKUP)
            val phone = StoredDailySteps(20, 0, StepSource.HEALTH_CONNECT)
            val imported = StoredDailySteps(21, 4_000, StepSource.BACKUP)
            val afterImport = listOf(backup, phone, imported)
            val rows = flowOf(emptyList(), listOf(backup), listOf(backup, phone), afterImport, afterImport)

            // When
            val stats = observeStats(rows.resolveDailySteps()).toList()

            // Then
            assertEquals(
                listOf(
                    StatsData(null, 0),
                    StatsData(DailySteps(20, 12_000, fromBackup = true), 12_000),
                    StatsData(DailySteps(20, 0, fromBackup = false), 0),
                    StatsData(DailySteps(21, 4_000, fromBackup = true), 4_000),
                ),
                stats,
            )
        }
}
