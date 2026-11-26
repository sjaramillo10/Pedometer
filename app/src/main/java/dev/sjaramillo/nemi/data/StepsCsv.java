package dev.sjaramillo.nemi.data;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

public final class StepsCsv {
    private StepsCsv() {}

    public record Import(List<DailySteps> dailySteps, int ignoredRows) {}

    public static String fileName(LocalDate date) {
        return "Nemi_" + date + ".csv";
    }

    public static String encode(List<DailySteps> entries) {
        var csv = new StringBuilder();
        entries.stream()
                .filter(entry -> DailySteps.isSupportedDay(entry.day()))
                .sorted(Comparator.comparingLong(DailySteps::day))
                .forEach(entry -> csv.append(entry.day()).append(',')
                        .append(Math.max(entry.steps(), 0)).append('\n'));
        return csv.toString();
    }

    public static Import decode(String csv) {
        var entries = new TreeMap<Long, DailySteps>();
        int ignored = 0;
        for (String line : csv.split("\\r\\n|\\r|\\n", -1)) {
            if (trim(line).isEmpty()) {
                continue;
            }
            String[] cells = line.split(",", 3);
            if (cells.length == 2) {
                try {
                    long day = Long.parseLong(trim(cells[0]));
                    long steps = Long.parseLong(trim(cells[1]));
                    if (DailySteps.isSupportedDay(day) && steps >= 0) {
                        entries.put(day, new DailySteps(day, steps, true));
                        continue;
                    }
                } catch (NumberFormatException ignoredNumber) {
                    // Count malformed numbers in the same way as malformed rows.
                }
            }
            ignored++;
        }
        return new Import(List.copyOf(entries.values()), ignored);
    }

    private static String trim(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isWhitespace(value.charAt(start))) {
            start++;
        }
        while (end > start && isWhitespace(value.charAt(end - 1))) {
            end--;
        }
        return value.substring(start, end);
    }

    private static boolean isWhitespace(char character) {
        return Character.isWhitespace(character) || Character.isSpaceChar(character);
    }
}
