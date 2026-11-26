package dev.sjaramillo.nemi.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.junit.Test;

public class HealthConnectSyncCoordinatorTest {
    private static final LocalDate DAY = LocalDate.of(2026, 9, 30);

    public static HealthConnectSyncCoordinator withoutPermission() {
        return coordinator(() -> PhoneStepsDataSource.Access.PermissionRequired,
                () -> { throw new AssertionError("Unexpected Health Connect read"); }, () -> {});
    }

    public static HealthConnectSyncCoordinator withCacheRefresh(Runnable write) {
        return coordinator(() -> PhoneStepsDataSource.Access.Available,
                HealthConnectSyncCoordinatorTest::completedWindow, write);
    }

    @Test
    public void givenMissingPermission_whenRefreshing_thenSkipsCacheSynchronization() {
        // Given
        var coordinator = withoutPermission();

        // When
        coordinator.refresh();

        // Then
        assertEquals(HealthConnectSyncState.PermissionRequired, coordinator.state());
    }

    @Test
    public void givenUnsupportedTracking_whenRefreshing_thenReportsReasonAndSkipsSynchronization() {
        // Given
        var unsupported = Map.of(
                PhoneStepsDataSource.Access.Unavailable, HealthConnectSyncState.Unavailable,
                PhoneStepsDataSource.Access.UpdateRequired, HealthConnectSyncState.UpdateRequired,
                PhoneStepsDataSource.Access.NoStepCounter, HealthConnectSyncState.NoStepCounter);

        unsupported.forEach((access, expected) -> {
            var coordinator = coordinator(() -> access,
                    () -> { throw new AssertionError("Unexpected Health Connect read"); }, () -> {});

            // When
            coordinator.refresh();

            // Then
            assertEquals(expected, coordinator.state());
        });
    }

    @Test
    public void givenAvailableHealthConnect_whenRefreshing_thenSyncsCacheAndBecomesReady() {
        // Given
        var synced = new AtomicBoolean();
        var coordinator = coordinator(() -> PhoneStepsDataSource.Access.Available,
                HealthConnectSyncCoordinatorTest::completedWindow, () -> synced.set(true));

        // When
        coordinator.refresh();

        // Then
        assertTrue(synced.get());
        assertEquals(HealthConnectSyncState.Ready, coordinator.state());
    }

    @Test
    public void givenAnAccessFailure_whenRefreshing_thenReportsAnError() {
        // Given
        var coordinator = coordinator(() -> { throw new IllegalStateException("Access failed"); },
                HealthConnectSyncCoordinatorTest::completedWindow, () -> {});

        // When
        coordinator.refresh();

        // Then
        assertEquals(HealthConnectSyncState.Error, coordinator.state());
    }

    @Test
    public void givenASyncFailure_whenRefreshing_thenReportsAnError() {
        // Given
        var coordinator = coordinator(() -> PhoneStepsDataSource.Access.Available,
                HealthConnectSyncCoordinatorTest::completedWindow,
                () -> { throw new IllegalStateException("Sync failed"); });

        // When
        coordinator.refresh();

        // Then
        assertEquals(HealthConnectSyncState.Error, coordinator.state());
    }

    @Test
    public void givenACancelledSync_whenTheReadReturns_thenSkipsTheWriteAndKeepsItsState() {
        // Given
        var pending = new CompletableFuture<HealthConnectStepCacheSynchronizer.Window>();
        var synced = new AtomicBoolean();
        var coordinator = coordinator(() -> PhoneStepsDataSource.Access.Available,
                () -> pending, () -> synced.set(true));
        coordinator.refresh();

        // When
        coordinator.cancel();
        pending.complete(completedWindow().join());

        // Then
        assertFalse(synced.get());
        assertFalse(coordinator.isActive());
        assertEquals(HealthConnectSyncState.Syncing, coordinator.state());
    }

    @Test
    public void givenAnEarlierRefreshStillPending_whenRetryCompletes_thenItsLateResultCannotOverwriteTheCache() {
        // Given
        var old = new CompletableFuture<HealthConnectStepCacheSynchronizer.Window>();
        var pending = new java.util.ArrayDeque<>(java.util.List.of(old, completedWindow()));
        var writes = new java.util.concurrent.atomic.AtomicInteger();
        var coordinator = coordinator(() -> PhoneStepsDataSource.Access.Available,
                pending::remove, writes::incrementAndGet);
        coordinator.refresh();

        // When
        coordinator.refresh();
        old.complete(completedWindow().join());

        // Then
        assertEquals(1, writes.get());
        assertEquals(HealthConnectSyncState.Ready, coordinator.state());
    }

    private static CompletableFuture<HealthConnectStepCacheSynchronizer.Window> completedWindow() {
        return CompletableFuture.completedFuture(
                new HealthConnectStepCacheSynchronizer.Window(DAY.minusDays(29), DAY, Map.of()));
    }

    private static HealthConnectSyncCoordinator coordinator(Supplier<PhoneStepsDataSource.Access> access,
            Supplier<CompletableFuture<HealthConnectStepCacheSynchronizer.Window>> reader, Runnable write) {
        return new HealthConnectSyncCoordinator(access, reader, ignored -> write.run(),
                Runnable::run, Runnable::run);
    }
}
