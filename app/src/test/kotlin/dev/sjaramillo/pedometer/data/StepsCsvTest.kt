package dev.sjaramillo.pedometer.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StepsCsvTest {
    @Test
    fun `encodes daily steps in ascending day order`() {
        assertEquals(
            "10,100\n20,200\n",
            encodeStepsCsv(
                listOf(
                    DailySteps(20, 200),
                    DailySteps(10, 100),
                ),
            ),
        )
    }

    @Test
    fun `decodes valid rows and ignores invalid rows`() {
        val imported =
            decodeStepsCsv(
                "20,200\ninvalid\n10,-1\n30,300\n",
            )

        assertEquals(
            listOf(DailySteps(20, 200), DailySteps(30, 300)),
            imported.dailySteps,
        )
        assertEquals(2, imported.ignoredRows)
    }
}
