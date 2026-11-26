package dev.sjaramillo.nemi.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertSame;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class HealthConnectStepCacheSynchronizerTest {
    private static final LocalDate FIRST_DAY = LocalDate.of(2026, 9, 30);

    @Test
    public void givenRepeatedReadsAcrossMidnight_whenSyncing_thenReadsEachCurrentWindowAndItsTotals() {
        // Given
        var nextDay = FIRST_DAY.plusDays(1);
        var date = new AtomicReference<>(FIRST_DAY);
        var totals = new AtomicReference<>(Map.of(FIRST_DAY, 500L, FIRST_DAY.minusDays(1), 300L));
        var ranges = new java.util.ArrayList<List<LocalDate>>();
        var synchronizer = new HealthConnectStepCacheSynchronizer(date::get, (start, end) -> {
            ranges.add(List.of(start, end));
            return CompletableFuture.completedFuture(totals.get());
        });

        // When
        var first = synchronizer.readWindow().join();
        date.set(nextDay);
        totals.set(Map.of(FIRST_DAY, 400L, nextDay, 0L));
        var next = synchronizer.readWindow().join();
        var repeated = synchronizer.readWindow().join();

        // Then
        var firstRange = List.of(FIRST_DAY.minusDays(29), FIRST_DAY);
        var nextRange = List.of(nextDay.minusDays(29), nextDay);
        assertEquals(List.of(firstRange, nextRange, nextRange), ranges);
        assertEquals(new HealthConnectStepCacheSynchronizer.Window(firstRange.get(0), FIRST_DAY,
                Map.of(FIRST_DAY, 500L, FIRST_DAY.minusDays(1), 300L)), first);
        assertEquals(new HealthConnectStepCacheSynchronizer.Window(nextRange.get(0), nextDay,
                Map.of(FIRST_DAY, 400L, nextDay, 0L)), next);
        assertEquals(next, repeated);
    }

    @Test
    public void givenAnEmptySuccessfulRead_whenSyncing_thenReturnsAnEmptyReplacementWindow() {
        // Given
        var synchronizer = new HealthConnectStepCacheSynchronizer(() -> FIRST_DAY,
                (start, end) -> CompletableFuture.completedFuture(Map.of()));

        // When
        var replacement = synchronizer.readWindow().join();

        // Then
        assertEquals(Map.of(), replacement.totals());
    }

    @Test
    public void givenAFailedHealthConnectRead_whenSyncing_thenReturnsNoReplacementWindow() {
        // Given
        var failure = new IllegalStateException("Health Connect read failed");
        var synchronizer = new HealthConnectStepCacheSynchronizer(() -> FIRST_DAY,
                (start, end) -> CompletableFuture.failedFuture(failure));

        // When
        var thrown = assertThrows(CompletionException.class, () -> synchronizer.readWindow().join());

        // Then
        assertSame(failure, thrown.getCause());
    }
}
