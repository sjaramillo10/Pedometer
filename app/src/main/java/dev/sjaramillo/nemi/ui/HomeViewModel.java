package dev.sjaramillo.nemi.ui;

import dev.sjaramillo.nemi.R;
import dev.sjaramillo.nemi.State;
import dev.sjaramillo.nemi.data.DailySteps;
import dev.sjaramillo.nemi.data.HealthConnectSyncCoordinator;
import dev.sjaramillo.nemi.data.HealthConnectSyncState;
import dev.sjaramillo.nemi.data.StepHistoryStats;
import dev.sjaramillo.nemi.data.StepsRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public final class HomeViewModel {
    private final NemiPreferences preferences;
    private final StepsHistory steps;
    private final HealthConnectSyncCoordinator coordinator;
    private final Executor worker;
    private final Executor main;
    private final Runnable stopGoal;
    private final Runnable stopSync;
    private final Runnable stopSteps;
    private final Supplier<LocalDate> clock;
    private LocalDate date;
    private LocalDate displayedDate;
    private DayOfWeek firstDay = WeekFields.of(Locale.getDefault()).getFirstDayOfWeek();
    private WeekSelection selection = new WeekSelection();
    private long dayRequest;
    private long weekRequest;
    private long historyRequest;
    private WeekRange requestedRange;
    private DailySteps today;
    private WeeklySteps week;
    private StepHistoryStats history;
    final State<HomeUiState> uiState;

    public HomeViewModel(NemiPreferences preferences, StepsRepository steps,
            HealthConnectSyncCoordinator coordinator, Executor worker, Executor main) {
        this(preferences, new StepsHistory() {
            @Override public Runnable observeChanges(Runnable observer) {
                return steps.observeChanges(observer);
            }
            @Override public List<DailySteps> readDays(LocalDate start, LocalDate end) {
                return steps.readDays(start, end);
            }
            @Override public StepHistoryStats historyStats() {
                return steps.historyStats();
            }
        }, coordinator, worker, main, LocalDate::now);
    }

    HomeViewModel(NemiPreferences preferences, StepsHistory steps,
            HealthConnectSyncCoordinator coordinator, Executor worker, Executor main,
            Supplier<LocalDate> clock) {
        this.clock = clock;
        date = displayedDate = clock.get();
        week = selection.range(date, firstDay).withEntries(List.of());
        this.preferences = preferences;
        this.steps = steps;
        this.coordinator = coordinator;
        this.worker = worker;
        this.main = main;
        uiState = new State<>(snapshot());
        stopGoal = preferences.goal.observe(ignored -> publish());
        stopSync = coordinator.observe(ignored -> publish());
        stopSteps = steps.observeChanges(() -> {
            readToday();
            readWeek(true);
            readHistory();
        });
    }

    void updateFirstDayOfWeek(DayOfWeek value) {
        if (firstDay != value) {
            firstDay = value;
            readWeek(false);
        }
    }

    void previousWeek() {
        selection = selection.previous(date, firstDay);
        readWeek(false);
    }

    void nextWeek() {
        WeekSelection next = selection.next(date, firstDay);
        if (!selection.equals(next)) {
            selection = next;
            readWeek(false);
        }
    }

    void refresh() {
        refresh(false);
    }

    void refresh(boolean restart) {
        LocalDate current = clock.get();
        if (!date.equals(current)) {
            date = current;
            readToday();
            readWeek(false);
        } else if (!restart && coordinator.isActive()) {
            return;
        }
        coordinator.refresh();
    }

    void close() {
        dayRequest++;
        weekRequest++;
        historyRequest++;
        coordinator.cancel();
        stopGoal.run();
        stopSync.run();
        stopSteps.run();
    }

    private void readToday() {
        long request = ++dayRequest;
        LocalDate day = date;
        worker.execute(() -> {
            var totals = steps.readDays(day, day);
            main.execute(() -> {
                if (request == dayRequest) {
                    displayedDate = day;
                    today = totals.isEmpty() ? null : totals.get(0);
                    publish();
                }
            });
        });
    }

    private void readWeek(boolean force) {
        WeekRange range = selection.range(date, firstDay);
        if (!force && range.equals(requestedRange)) {
            return;
        }
        requestedRange = range;
        long request = ++weekRequest;
        worker.execute(() -> {
            var days = range.withEntries(steps.readDays(range.start(), range.through()));
            main.execute(() -> {
                if (request == weekRequest) {
                    week = days;
                    publish();
                }
            });
        });
    }

    private void readHistory() {
        long request = ++historyRequest;
        worker.execute(() -> {
            var stats = steps.historyStats();
            main.execute(() -> {
                if (request == historyRequest) {
                    history = stats;
                    publish();
                }
            });
        });
    }

    private void publish() {
        uiState.set(snapshot());
    }

    private HomeUiState snapshot() {
        return new HomeUiState(displayedDate, week, today, preferences.goal.value(), coordinator.state(),
                history == null ? null : history.bestDay(),
                history == null ? null : history.lifetimeSteps());
    }
    interface StepsHistory {
        Runnable observeChanges(Runnable observer);
        List<DailySteps> readDays(LocalDate start, LocalDate end);
        StepHistoryStats historyStats();
    }
}

record HomeUiState(LocalDate date, WeeklySteps week, DailySteps today, int goal,
        HealthConnectSyncState sync, DailySteps bestDay, Long lifetimeSteps) {
    Integer todayEmptyMessage() {
        if (today != null) {
            return null;
        }
        return sync == HealthConnectSyncState.Ready ? R.string.no_steps_today : R.string.no_step_total_today;
    }

    float progress() {
        return (float) Math.max(0, Math.min(1, (today == null ? 0 : (double) today.steps()) / goal));
    }

    boolean reachedGoal(long steps) {
        return steps >= goal;
    }
}
