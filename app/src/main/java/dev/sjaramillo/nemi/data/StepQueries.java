package dev.sjaramillo.nemi.data;

final class StepQueries {
    private StepQueries() {}

    static final String CREATE_DAILY_STEPS =
            "CREATE TABLE IF NOT EXISTS daily_steps (day INTEGER NOT NULL, steps INTEGER NOT NULL, "
                    + "source TEXT NOT NULL, PRIMARY KEY (day, source))";

    private static final String RESOLVED_DAYS = """
            SELECT entry.day, entry.steps, entry.source = 'BACKUP' AS fromBackup
            FROM daily_steps AS entry
            WHERE entry.day BETWEEN :firstDay AND :lastDay
            AND (entry.source = 'HEALTH_CONNECT' OR NOT EXISTS (
                SELECT 1 FROM daily_steps AS preferred
                WHERE preferred.day = entry.day AND preferred.source = 'HEALTH_CONNECT'
            ))
            """;

    static final String RESOLVED_DAYS_IN_ORDER = RESOLVED_DAYS + " ORDER BY entry.day";

    // Sum the 32-bit halves separately to preserve long arithmetic for very large CSV totals.
    static final String RESOLVED_STEP_STATS = """
            WITH resolved AS (%s),
                 best AS (SELECT * FROM resolved ORDER BY steps DESC, day LIMIT 1)
            SELECT ((totals.high + (totals.low >> 32)) << 32) | (totals.low & 4294967295) AS lifetimeSteps,
                   best.day AS best_day, best.steps AS best_steps, best.fromBackup AS best_fromBackup
            FROM (
                SELECT COALESCE(SUM(steps >> 32), 0) AS high, COALESCE(SUM(steps & 4294967295), 0) AS low
                FROM resolved
            ) AS totals
            LEFT JOIN best ON 1 = 1
            """.formatted(RESOLVED_DAYS);

    static String boundQuery(String query) {
        return query.replace(":firstDay", "?").replace(":lastDay", "?");
    }
}
