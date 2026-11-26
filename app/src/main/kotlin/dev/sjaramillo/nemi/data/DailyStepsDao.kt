package dev.sjaramillo.nemi.data

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
    val bestDay: DailySteps?,
)
