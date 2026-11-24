package dev.sjaramillo.nemi.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class StepsCsvTest {
    @Test
    fun `given unsupported epoch days when importing then ignores them and keeps valid boundary dates`() {
        // Given
        val lastDay = LocalDate.MAX.toEpochDay()
        val csv = "1,100\n0,200\n$lastDay,300\n${lastDay + 1},400\n1795392000000,1234\n"

        // When
        val imported = decodeStepsCsv(csv)

        // Then
        assertEquals(
            listOf(DailySteps(1, 100, fromBackup = true), DailySteps(lastDay, 300, fromBackup = true)),
            imported.dailySteps,
        )
        assertEquals(3, imported.ignoredRows)
    }

    @Test
    fun `given unsorted totals from both sources when exporting then writes two columns in ascending day order`() {
        // Given
        val totals = listOf(DailySteps(20, 200, fromBackup = true), DailySteps(10, 100, fromBackup = false))

        // When
        val exported = encodeStepsCsv(totals)

        // Then
        assertEquals("10,100\n20,200\n", exported)
    }

    @Test
    fun `given valid and invalid CSV rows when importing then marks valid rows as backup totals`() {
        // Given
        val csv = "20,200\ninvalid\n10,-1\n30,300\n"

        // When
        val imported = decodeStepsCsv(csv)

        // Then
        assertEquals(
            listOf(DailySteps(20, 200, fromBackup = true), DailySteps(30, 300, fromBackup = true)),
            imported.dailySteps,
        )
        assertEquals(2, imported.ignoredRows)
    }
}
