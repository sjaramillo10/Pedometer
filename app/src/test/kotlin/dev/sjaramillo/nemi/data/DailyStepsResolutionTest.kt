package dev.sjaramillo.nemi.data

import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DailyStepsResolutionTest {
    @Test
    fun `given overlapping sources when querying a range then Health Connect zero wins and backup provenance is retained`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { database ->
            // Given
            database.seed(
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
                database.read(RESOLVED_DAYS_IN_ORDER, 10, 12) { rows ->
                    buildList {
                        while (rows.next()) {
                            add(DailySteps(rows.getLong("day"), rows.getLong("steps"), rows.getBoolean("fromBackup")))
                        }
                    }
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
    }

    @Test
    fun `given duplicate sources and tied records when querying history stats then sum uses resolved days and earliest best day wins`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { database ->
            // Given
            database.seed(
                StoredDailySteps(0, 9999, StepSource.BACKUP),
                StoredDailySteps(10, 900, StepSource.BACKUP),
                StoredDailySteps(10, 0, StepSource.HEALTH_CONNECT),
                StoredDailySteps(11, 500, StepSource.BACKUP),
                StoredDailySteps(12, 500, StepSource.HEALTH_CONNECT),
                StoredDailySteps(lastSupportedEpochDay + 1, 9999, StepSource.BACKUP),
            )

            // When
            database.read(RESOLVED_STEP_STATS, 1, lastSupportedEpochDay) { rows ->
                // Then
                assertTrue(rows.next())
                assertEquals(1000L, rows.getLong("lifetimeSteps"))
                assertEquals(11L, rows.getLong("best_day"))
                assertEquals(500L, rows.getLong("best_steps"))
                assertTrue(rows.getBoolean("best_fromBackup"))
            }
        }
    }

    @Test
    fun `given empty history when querying stats then lifetime is zero and the best day is absent`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { database ->
            // Given
            database.seed()

            // When
            database.read(RESOLVED_STEP_STATS, 1, lastSupportedEpochDay) { rows ->
                // Then
                assertTrue(rows.next())
                assertEquals(0L, rows.getLong("lifetimeSteps"))
                assertNull(rows.getObject("best_day"))
                assertNull(rows.getObject("best_steps"))
                assertNull(rows.getObject("best_fromBackup"))
            }
        }
    }

    @Test
    fun `given very large CSV totals when querying lifetime steps then the result preserves Long arithmetic`() {
        DriverManager.getConnection("jdbc:sqlite::memory:").use { database ->
            // Given
            database.seed(
                StoredDailySteps(10, Long.MAX_VALUE - 1, StepSource.BACKUP),
                StoredDailySteps(11, 3, StepSource.BACKUP),
            )

            // When
            database.read(RESOLVED_STEP_STATS, 1, lastSupportedEpochDay) { rows ->
                // Then
                assertTrue(rows.next())
                assertEquals(Long.MIN_VALUE + 1, rows.getLong("lifetimeSteps"))
            }
        }
    }

    private fun Connection.seed(vararg entries: StoredDailySteps) {
        createStatement().use {
            it.execute(
                "CREATE TABLE daily_steps (day INTEGER NOT NULL, steps INTEGER NOT NULL, source TEXT NOT NULL, PRIMARY KEY (day, source))",
            )
        }
        prepareStatement("INSERT INTO daily_steps (day, steps, source) VALUES (?, ?, ?)").use { statement ->
            entries.forEach { entry ->
                statement.setLong(1, entry.day)
                statement.setLong(2, entry.steps)
                statement.setString(3, entry.source.name)
                statement.executeUpdate()
            }
        }
    }

    private fun <T> Connection.read(
        query: String,
        firstDay: Long,
        lastDay: Long,
        readRows: (ResultSet) -> T,
    ): T =
        prepareStatement(query.replace(":firstDay", "?").replace(":lastDay", "?")).use { statement ->
            statement.setLong(1, firstDay)
            statement.setLong(2, lastDay)
            statement.executeQuery().use(readRows)
        }
}
