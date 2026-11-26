package dev.sjaramillo.nemi.data;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Supplier;

final class HealthConnectStepCacheSynchronizer {
    private static final int RECENT_HISTORY_DAYS = 30;

    private final Supplier<LocalDate> currentDate;
    private final BiFunction<LocalDate, LocalDate,
            CompletableFuture<Map<LocalDate, Long>>> readDailySteps;

    HealthConnectStepCacheSynchronizer(Supplier<LocalDate> currentDate,
            BiFunction<LocalDate, LocalDate, CompletableFuture<Map<LocalDate, Long>>> readDailySteps) {
        this.currentDate = currentDate;
        this.readDailySteps = readDailySteps;
    }

    CompletableFuture<Window> readWindow() {
        LocalDate end = currentDate.get();
        LocalDate start = end.minusDays(RECENT_HISTORY_DAYS - 1);
        return readDailySteps.apply(start, end).thenApply(totals -> new Window(start, end, totals));
    }

    record Window(LocalDate start, LocalDate end, Map<LocalDate, Long> totals) {}
}
