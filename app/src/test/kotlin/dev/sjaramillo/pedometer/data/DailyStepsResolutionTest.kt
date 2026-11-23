package dev.sjaramillo.pedometer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class DailyStepsResolutionTest {
    @Test
    fun `given overlapping totals when exporting resolved days then Health Connect wins including zero`() {
        // Given
        val entries =
            listOf(
                StoredDailySteps(10, 900, StepSource.CSV),
                StoredDailySteps(10, 0, StepSource.HEALTH_CONNECT),
                StoredDailySteps(11, 500, StepSource.HEALTH_CONNECT),
                StoredDailySteps(11, 1000, StepSource.CSV),
                StoredDailySteps(12, 4200, StepSource.CSV),
            )

        // When
        val csv = encodeStepsCsv(resolveDailySteps(entries))

        // Then
        assertEquals("10,0\n11,500\n12,4200\n", csv)
    }
}
