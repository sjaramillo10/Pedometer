package dev.sjaramillo.nemi.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

class DailyStepsResolutionTest {
    @Test
    fun `given overlapping sources when querying a range then Health Connect zero wins and backup provenance is retained`() {
        // Given
        val entries =
            listOf(
                StoredDailySteps(9, 8_000, StepSource.BACKUP),
                StoredDailySteps(10, 900, StepSource.BACKUP),
                StoredDailySteps(10, 0, StepSource.HEALTH_CONNECT),
                StoredDailySteps(11, 500, StepSource.HEALTH_CONNECT),
                StoredDailySteps(11, 1000, StepSource.BACKUP),
                StoredDailySteps(12, 4200, StepSource.BACKUP),
                StoredDailySteps(13, 7_000, StepSource.BACKUP),
            )

        // When
        val resolved =
            read(entries, RESOLVED_DAYS_IN_ORDER, 10, 12).map { row ->
                val (day, steps, fromBackup) = row.split('|')
                DailySteps(day.toLong(), steps.toLong(), fromBackup == "1")
            }

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

    @Test
    fun `given duplicate sources and tied records when querying history stats then sum uses resolved days and earliest best day wins`() {
        // Given
        val entries =
            listOf(
                StoredDailySteps(0, 9999, StepSource.BACKUP),
                StoredDailySteps(10, 900, StepSource.BACKUP),
                StoredDailySteps(10, 0, StepSource.HEALTH_CONNECT),
                StoredDailySteps(11, 500, StepSource.BACKUP),
                StoredDailySteps(12, 500, StepSource.HEALTH_CONNECT),
                StoredDailySteps(lastSupportedEpochDay + 1, 9999, StepSource.BACKUP),
            )

        // When
        val rows = read(entries, RESOLVED_STEP_STATS, 1, lastSupportedEpochDay)

        // Then
        assertEquals(listOf("1000|11|500|1"), rows)
    }

    @Test
    fun `given empty history when querying stats then lifetime is zero and the best day is absent`() {
        // Given
        val entries = emptyList<StoredDailySteps>()

        // When
        val rows = read(entries, RESOLVED_STEP_STATS, 1, lastSupportedEpochDay)

        // Then
        assertEquals(listOf("0|NULL|NULL|NULL"), rows)
    }

    @Test
    fun `given very large CSV totals when querying lifetime steps then the result preserves Long arithmetic`() {
        // Given
        val entries =
            listOf(
                StoredDailySteps(10, Long.MAX_VALUE - 1, StepSource.BACKUP),
                StoredDailySteps(11, 3, StepSource.BACKUP),
            )

        // When
        val rows = read(entries, RESOLVED_STEP_STATS, 1, lastSupportedEpochDay)

        // Then
        assertEquals(listOf("${Long.MIN_VALUE + 1}|10|${Long.MAX_VALUE - 1}|1"), rows)
    }

    private fun read(
        entries: List<StoredDailySteps>,
        query: String,
        firstDay: Long,
        lastDay: Long,
    ): List<String> {
        val process =
            try {
                ProcessBuilder("sqlite3", "-batch", "-bail", "-list", "-nullvalue", "NULL", ":memory:")
                    .redirectErrorStream(true)
                    .start()
            } catch (error: IOException) {
                throw AssertionError("Install the sqlite3 CLI and add it to PATH to run SQL regression tests.", error)
            }
        val sql =
            buildString {
                appendLine("$CREATE_DAILY_STEPS;")
                entries.forEach { entry ->
                    appendLine("INSERT INTO daily_steps VALUES (${entry.day}, ${entry.steps}, '${entry.source.name}');")
                }
                appendLine(query.replace(":firstDay", firstDay.toString()).replace(":lastDay", lastDay.toString()) + ";")
            }
        process.outputStream.bufferedWriter().use { it.write(sql) }
        val rows = process.inputStream.bufferedReader().use { it.readLines() }
        assertEquals(rows.joinToString("\n"), 0, process.waitFor())
        return rows
    }
}
