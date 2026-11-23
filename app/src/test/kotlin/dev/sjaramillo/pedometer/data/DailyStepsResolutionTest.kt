package dev.sjaramillo.pedometer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyStepsResolutionTest {
    @Test
    fun `given overlapping totals when resolving days then Health Connect wins and backup provenance is retained`() {
        // Given
        val entries =
            listOf(
                StoredDailySteps(10, 900, StepSource.BACKUP),
                StoredDailySteps(10, 0, StepSource.HEALTH_CONNECT),
                StoredDailySteps(11, 500, StepSource.HEALTH_CONNECT),
                StoredDailySteps(11, 1000, StepSource.BACKUP),
                StoredDailySteps(12, 4200, StepSource.BACKUP),
            )

        // When
        val resolved = resolveDailySteps(entries)

        // Then
        assertEquals(
            listOf(
                DailySteps(10, 0, fromBackup = false),
                DailySteps(11, 500, fromBackup = false),
                DailySteps(12, 4200, fromBackup = true),
            ),
            resolved,
        )
        assertEquals("10,0\n11,500\n12,4200\n", encodeStepsCsv(resolved))
    }
}
