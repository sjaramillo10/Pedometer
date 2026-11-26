package dev.sjaramillo.nemi.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import dev.sjaramillo.nemi.State;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

public final class StepsRepository {
    private final NemiDatabase database;
    private final Executor main;
    private final State<Long> revision = new State<>(0L);

    public StepsRepository(NemiDatabase database, Executor main) {
        this.database = database;
        this.main = main;
    }

    public Runnable observeChanges(Runnable observer) {
        return revision.observe(ignored -> observer.run());
    }

    public List<DailySteps> getAll() {
        return readDays(1, DailySteps.LAST_SUPPORTED_EPOCH_DAY);
    }

    public List<DailySteps> readDays(LocalDate start, LocalDate end) {
        return readDays(Math.max(start.toEpochDay(), 1), end.toEpochDay());
    }

    private List<DailySteps> readDays(long firstDay, long lastDay) {
        var entries = new ArrayList<DailySteps>();
        try (Cursor rows = query(StepQueries.RESOLVED_DAYS_IN_ORDER, firstDay, lastDay)) {
            while (rows.moveToNext()) {
                entries.add(day(rows, 0));
            }
        }
        return List.copyOf(entries);
    }

    public StepHistoryStats historyStats() {
        try (Cursor rows = query(StepQueries.RESOLVED_STEP_STATS, 1,
                DailySteps.LAST_SUPPORTED_EPOCH_DAY)) {
            if (!rows.moveToFirst()) {
                throw new IllegalStateException("The history stats query returned no rows");
            }
            return new StepHistoryStats(rows.getLong(0), rows.isNull(1) ? null : day(rows, 1));
        }
    }

    public void importDailySteps(List<DailySteps> dailySteps) {
        write(connection -> insertAll(connection,
                dailySteps.stream().map(entry -> new Entry(entry.day(), entry.steps())).toList(),
                "BACKUP"));
    }

    void replaceHealthConnectWindow(LocalDate start, LocalDate end, Map<LocalDate, Long> totals) {
        write(connection -> {
            connection.delete("daily_steps", "source = ? AND day BETWEEN ? AND ?",
                    new String[] {"HEALTH_CONNECT", Long.toString(start.toEpochDay()),
                            Long.toString(end.toEpochDay())});
            var entries = totals.entrySet().stream()
                    .map(total -> new Entry(total.getKey().toEpochDay(), total.getValue())).toList();
            insertAll(connection, entries, "HEALTH_CONNECT");
        });
    }

    private static DailySteps day(Cursor rows, int column) {
        return new DailySteps(rows.getLong(column), rows.getLong(column + 1),
                rows.getInt(column + 2) != 0);
    }

    private Cursor query(String sql, long firstDay, long lastDay) {
        return database.getReadableDatabase().rawQuery(StepQueries.boundQuery(sql),
                new String[] {Long.toString(firstDay), Long.toString(lastDay)});
    }

    private void write(Consumer<SQLiteDatabase> operation) {
        var connection = database.getWritableDatabase();
        connection.beginTransactionNonExclusive();
        try {
            operation.accept(connection);
            connection.setTransactionSuccessful();
        } finally {
            connection.endTransaction();
        }
        main.execute(() -> revision.set(revision.value() + 1));
    }

    private static void insertAll(SQLiteDatabase connection, List<Entry> entries, String source) {
        try (var statement = connection.compileStatement(
                "INSERT OR REPLACE INTO daily_steps (day, steps, source) VALUES (?, ?, ?)")) {
            for (Entry entry : entries) {
                statement.bindLong(1, entry.day());
                statement.bindLong(2, entry.steps());
                statement.bindString(3, source);
                statement.executeInsert();
            }
        }
    }

    private record Entry(long day, long steps) {}
}
