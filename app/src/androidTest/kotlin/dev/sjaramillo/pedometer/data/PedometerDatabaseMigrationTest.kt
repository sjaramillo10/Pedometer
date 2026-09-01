package dev.sjaramillo.pedometer.data

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PedometerDatabaseMigrationTest {
    @get:Rule
    val migrationTestHelper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            PedometerDatabase::class.java.name,
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun migrationFromVersion1_clearsExistingStepHistory() {
        migrationTestHelper.createDatabase(TEST_DATABASE_NAME, 1).apply {
            execSQL("INSERT INTO daily_steps (day, steps) VALUES (20000, 1234)")
            close()
        }

        migrationTestHelper
            .runMigrationsAndValidate(
                TEST_DATABASE_NAME,
                2,
                true,
                PedometerDatabase.MIGRATION_1_2,
            ).use { database ->
                database.query("SELECT COUNT(*) FROM daily_steps").use { cursor ->
                    cursor.moveToFirst()
                    assertEquals(0, cursor.getInt(0))
                }
            }
    }

    private companion object {
        const val TEST_DATABASE_NAME = "pedometer-migration-test"
    }
}
