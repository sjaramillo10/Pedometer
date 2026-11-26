package dev.sjaramillo.nemi.data;

import static dev.sjaramillo.nemi.data.DailySteps.LAST_SUPPORTED_EPOCH_DAY;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

public class DailyStepsResolutionTest {
    @Test
    public void givenOverlappingSources_whenQueryingARange_thenHealthConnectZeroWinsAndBackupProvenanceIsRetained() throws Exception {
        // Given
        var entries = List.of(
                new Entry(9, 8_000, "BACKUP"),
                new Entry(10, 900, "BACKUP"),
                new Entry(10, 0, "HEALTH_CONNECT"),
                new Entry(11, 500, "HEALTH_CONNECT"),
                new Entry(11, 1000, "BACKUP"),
                new Entry(12, 4200, "BACKUP"),
                new Entry(13, 7_000, "BACKUP"));

        // When
        var resolved = read(entries, StepQueries.RESOLVED_DAYS_IN_ORDER, 10, 12).stream()
                .map(row -> {
                    String[] columns = row.split("\\|");
                    return new DailySteps(Long.parseLong(columns[0]), Long.parseLong(columns[1]),
                            columns[2].equals("1"));
                }).toList();

        // Then
        assertEquals(List.of(new DailySteps(10, 0, false), new DailySteps(11, 500, false),
                new DailySteps(12, 4200, true)), resolved);
        assertEquals("10,0\n11,500\n12,4200\n", StepsCsv.encode(resolved));
    }

    @Test
    public void givenDuplicateSourcesAndTiedRecords_whenQueryingHistoryStats_thenUsesResolvedDaysAndEarliestBestDay() throws Exception {
        // Given
        var entries = List.of(
                new Entry(0, 9999, "BACKUP"),
                new Entry(10, 900, "BACKUP"),
                new Entry(10, 0, "HEALTH_CONNECT"),
                new Entry(11, 500, "BACKUP"),
                new Entry(12, 500, "HEALTH_CONNECT"),
                new Entry(LAST_SUPPORTED_EPOCH_DAY + 1, 9999, "BACKUP"));

        // When
        var rows = read(entries, StepQueries.RESOLVED_STEP_STATS, 1, LAST_SUPPORTED_EPOCH_DAY);

        // Then
        assertEquals(List.of("1000|11|500|1"), rows);
    }

    @Test
    public void givenEmptyHistory_whenQueryingStats_thenLifetimeIsZeroAndBestDayIsAbsent() throws Exception {
        // Given
        List<Entry> entries = List.of();

        // When
        var rows = read(entries, StepQueries.RESOLVED_STEP_STATS, 1, LAST_SUPPORTED_EPOCH_DAY);

        // Then
        assertEquals(List.of("0|NULL|NULL|NULL"), rows);
    }

    @Test
    public void givenVeryLargeCsvTotals_whenQueryingLifetimeSteps_thenPreservesLongArithmetic() throws Exception {
        // Given
        var entries = List.of(new Entry(10, Long.MAX_VALUE - 1, "BACKUP"),
                new Entry(11, 3, "BACKUP"));

        // When
        var rows = read(entries, StepQueries.RESOLVED_STEP_STATS, 1, LAST_SUPPORTED_EPOCH_DAY);

        // Then
        assertEquals(List.of((Long.MIN_VALUE + 1) + "|10|" + (Long.MAX_VALUE - 1) + "|1"), rows);
    }

    private static List<String> read(List<Entry> entries, String query, long first, long last)
            throws IOException, InterruptedException {
        var sql = new StringBuilder(StepQueries.CREATE_DAILY_STEPS).append(";\n");
        for (Entry entry : entries) {
            sql.append("INSERT INTO daily_steps VALUES (").append(entry.day()).append(", ")
                    .append(entry.steps()).append(", '").append(entry.source()).append("');\n");
        }
        sql.append(query.replace(":firstDay", Long.toString(first))
                .replace(":lastDay", Long.toString(last))).append(";\n");
        Process process;
        try {
            process = new ProcessBuilder("sqlite3", "-batch", "-bail", "-list", "-nullvalue",
                    "NULL", ":memory:").redirectErrorStream(true).start();
        } catch (IOException error) {
            throw new AssertionError("Install the sqlite3 CLI and add it to PATH to run SQL regression tests.", error);
        }
        try (var input = process.getOutputStream()) {
            input.write(sql.toString().getBytes(StandardCharsets.UTF_8));
        }
        String output;
        try (var result = process.getInputStream()) {
            output = new String(result.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(output, 0, process.waitFor());
        return output.lines().toList();
    }

    private record Entry(long day, long steps, String source) {}
}
