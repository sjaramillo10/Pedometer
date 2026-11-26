package dev.sjaramillo.nemi.data;

import dev.sjaramillo.nemi.State;
import java.time.LocalDate;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class HealthConnectSyncCoordinator {
    private final Supplier<PhoneStepsDataSource.Access> checkAccess;
    private final Supplier<CompletableFuture<HealthConnectStepCacheSynchronizer.Window>> readWindow;
    private final Consumer<HealthConnectStepCacheSynchronizer.Window> replaceWindow;
    private final Executor worker;
    private final Executor main;
    private final State<HealthConnectSyncState> state = new State<>(HealthConnectSyncState.Loading);
    private volatile long generation;
    private boolean active;

    public HealthConnectSyncCoordinator(PhoneStepsDataSource source, StepsRepository steps,
            Executor worker, Executor main) {
        this(source::access,
                new HealthConnectStepCacheSynchronizer(LocalDate::now, source::readDailySteps)::readWindow,
                window -> steps.replaceHealthConnectWindow(window.start(), window.end(), window.totals()),
                worker, main);
    }

    HealthConnectSyncCoordinator(Supplier<PhoneStepsDataSource.Access> checkAccess,
            Supplier<CompletableFuture<HealthConnectStepCacheSynchronizer.Window>> readWindow,
            Consumer<HealthConnectStepCacheSynchronizer.Window> replaceWindow,
            Executor worker, Executor main) {
        this.checkAccess = checkAccess;
        this.readWindow = readWindow;
        this.replaceWindow = replaceWindow;
        this.worker = worker;
        this.main = main;
    }

    public HealthConnectSyncState state() {
        return state.value();
    }

    public Runnable observe(Consumer<HealthConnectSyncState> observer) {
        return state.observe(observer);
    }

    public boolean isActive() {
        return active;
    }

    public void cancel() {
        generation++;
        active = false;
    }

    public void refresh() {
        long request = ++generation;
        active = true;
        state.set(HealthConnectSyncState.Loading);
        try {
            switch (checkAccess.get()) {
                case PermissionRequired -> finish(request, HealthConnectSyncState.PermissionRequired);
                case Unavailable -> finish(request, HealthConnectSyncState.Unavailable);
                case UpdateRequired -> finish(request, HealthConnectSyncState.UpdateRequired);
                case NoStepCounter -> finish(request, HealthConnectSyncState.NoStepCounter);
                case Available -> {
                    state.set(HealthConnectSyncState.Syncing);
                    readWindow.get()
                            .thenAcceptAsync(window -> {
                                if (request == generation) {
                                    replaceWindow.accept(window);
                                }
                            }, worker)
                            .whenCompleteAsync((ignored, error) -> finish(request,
                                    error == null ? HealthConnectSyncState.Ready
                                            : HealthConnectSyncState.Error), main);
                }
            }
        } catch (Exception error) {
            finish(request, HealthConnectSyncState.Error);
        }
    }

    private void finish(long request, HealthConnectSyncState result) {
        if (request == generation) {
            active = false;
            state.set(result);
        }
    }
}
