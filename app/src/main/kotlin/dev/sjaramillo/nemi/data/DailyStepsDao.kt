package dev.sjaramillo.nemi.data

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

private const val RESOLVED_DAYS = """
    SELECT entry.day, entry.steps, entry.source = 'BACKUP' AS fromBackup
    FROM daily_steps AS entry
    WHERE entry.day BETWEEN :firstDay AND :lastDay
    AND (entry.source = 'HEALTH_CONNECT' OR NOT EXISTS (
        SELECT 1 FROM daily_steps AS preferred
        WHERE preferred.day = entry.day AND preferred.source = 'HEALTH_CONNECT'
    ))
"""

internal const val RESOLVED_DAYS_IN_ORDER = "$RESOLVED_DAYS ORDER BY entry.day"

// Sum each 32-bit half before recombining to preserve Long arithmetic even for very large CSV totals.
internal const val RESOLVED_STEP_STATS = """
    WITH resolved AS ($RESOLVED_DAYS),
         best AS (SELECT * FROM resolved ORDER BY steps DESC, day LIMIT 1)
    SELECT ((totals.high + (totals.low >> 32)) << 32) | (totals.low & 4294967295) AS lifetimeSteps,
           best.day AS best_day, best.steps AS best_steps, best.fromBackup AS best_fromBackup
    FROM (
        SELECT COALESCE(SUM(steps >> 32), 0) AS high, COALESCE(SUM(steps & 4294967295), 0) AS low
        FROM resolved
    ) AS totals
    LEFT JOIN best ON 1 = 1
"""

data class StepHistoryStats(
    val lifetimeSteps: Long,
    @Embedded(prefix = "best_") val bestDay: DailySteps?,
)

@Dao
interface DailyStepsDao {
    @Query(RESOLVED_DAYS_IN_ORDER)
    suspend fun getResolvedDays(
        firstDay: Long,
        lastDay: Long,
    ): List<DailySteps>

    @Query(RESOLVED_DAYS_IN_ORDER)
    fun observeResolvedDays(
        firstDay: Long,
        lastDay: Long,
    ): Flow<List<DailySteps>>

    @Query(RESOLVED_STEP_STATS)
    fun observeHistoryStats(
        firstDay: Long,
        lastDay: Long,
    ): Flow<StepHistoryStats>

    @Query("DELETE FROM daily_steps WHERE source = :source AND day BETWEEN :start AND :end")
    suspend fun deleteFrom(
        source: StepSource,
        start: Long,
        end: Long,
    )

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<StoredDailySteps>)
}
