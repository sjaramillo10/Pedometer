package dev.sjaramillo.nemi.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import java.time.LocalDate

class HealthConnectStepCacheSynchronizerTest {
    @Test
    fun `given repeated reads across midnight when syncing then replaces each current window with reported totals`() =
        runBlocking {
            // Given
            var today = LocalDate.of(2026, 9, 30)
            val firstDay = today
            val nextDay = firstDay.plusDays(1)
            val requestedDays = mutableListOf<Pair<LocalDate, LocalDate>>()
            val replacements = mutableListOf<Triple<LocalDate, LocalDate, Map<LocalDate, Long>>>()
            var totals: Map<LocalDate, Long> = mapOf(firstDay to 500, firstDay.minusDays(1) to 300)
            val synchronizer =
                HealthConnectStepCacheSynchronizer(
                    { today },
                    { start, end ->
                        requestedDays.add(start to end)
                        totals
                    },
                    { start, end, entries -> replacements.add(Triple(start, end, entries)) },
                )

            // When
            synchronizer.sync()
            today = nextDay
            totals = mapOf(firstDay to 400, nextDay to 0)
            synchronizer.sync()
            synchronizer.sync()

            // Then
            val firstWindow = firstDay.minusDays(29) to firstDay
            val nextWindow = nextDay.minusDays(29) to nextDay
            assertEquals(listOf(firstWindow, nextWindow, nextWindow), requestedDays)
            assertEquals(Triple(firstWindow.first, firstDay, mapOf(firstDay to 500L, firstDay.minusDays(1) to 300L)), replacements[0])
            assertEquals(Triple(nextWindow.first, nextDay, mapOf(firstDay to 400L, nextDay to 0L)), replacements[1])
            assertEquals(replacements[1], replacements[2])
        }

    @Test
    fun `given an empty successful read when syncing then replaces the window with no Health Connect rows`() =
        runBlocking {
            // Given
            val today = LocalDate.of(2026, 9, 30)
            val replacements = mutableListOf<Map<LocalDate, Long>>()
            val synchronizer =
                HealthConnectStepCacheSynchronizer(
                    { today },
                    { _, _ -> emptyMap() },
                    { _, _, entries -> replacements.add(entries) },
                )

            // When
            synchronizer.sync()

            // Then
            assertEquals(listOf(emptyMap<LocalDate, Long>()), replacements)
        }

    @Test
    fun `given a failed Health Connect read when syncing then leaves the stored window intact`() =
        runBlocking {
            // Given
            val failure = IllegalStateException("Health Connect read failed")
            var replaced = false
            val synchronizer =
                HealthConnectStepCacheSynchronizer(
                    { LocalDate.of(2026, 9, 30) },
                    { _, _ -> throw failure },
                    { _, _, _ -> replaced = true },
                )

            // When
            val thrown = runCatching { synchronizer.sync() }.exceptionOrNull()

            // Then
            assertSame(failure, thrown)
            assertFalse(replaced)
        }
}
