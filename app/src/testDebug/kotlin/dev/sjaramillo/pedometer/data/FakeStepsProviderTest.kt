package dev.sjaramillo.pedometer.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class FakeStepsProviderTest {
    @Test
    fun `generates inclusive deterministic daily history`() =
        runBlocking {
            val provider = FakeStepsProvider()
            val start = LocalDate.ofEpochDay(0)
            val result = provider.readDailySteps(start, start.plusDays(29))

            assertEquals(30, result.size)
            assertEquals(1500L, result[start])
            assertEquals(14000L, result[start.plusDays(29)])
            assertEquals(result, provider.readDailySteps(start, start.plusDays(29)))
            assertEquals(result[start.plusDays(10)], provider.readDailySteps(start.plusDays(10), start.plusDays(10))[start.plusDays(10)])
        }

    @Test
    fun `empty reversed ranges and pre epoch dates are supported`() =
        runBlocking {
            val provider = FakeStepsProvider()
            val start = LocalDate.ofEpochDay(-1)

            assertEquals(emptyMap<LocalDate, Long>(), provider.readDailySteps(start, start.minusDays(1)))
            assertEquals(mapOf(start to 14000L), provider.readDailySteps(start, start))
            assertEquals(StepsAccess.Available, provider.access())
        }
}
