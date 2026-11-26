package dev.sjaramillo.nemi.ui;

import static dev.sjaramillo.nemi.data.HealthConnectSyncCoordinatorTest.withoutPermission;
import static dev.sjaramillo.nemi.data.HealthConnectSyncCoordinatorTest.withCacheRefresh;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SUNDAY;
import static org.junit.Assert.*;

import dev.sjaramillo.nemi.R;
import dev.sjaramillo.nemi.data.DailySteps;
import dev.sjaramillo.nemi.data.HealthConnectSyncCoordinator;
import dev.sjaramillo.nemi.data.HealthConnectSyncState;
import dev.sjaramillo.nemi.data.StepHistoryStats;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import org.junit.Test;

public class HomeUiStateTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 9, 27);
    private static final DailySteps CURRENT = new DailySteps(TODAY.toEpochDay(), 10_000, false);

    @Test
    public void givenNoSavedSteps_whenTrackingIsUnsupportedOrReady_thenTodayExplainsTheMissingTotal() {
        // Given
        var unsupported = List.of(HealthConnectSyncState.Unavailable, HealthConnectSyncState.UpdateRequired,
                HealthConnectSyncState.NoStepCounter);
        var ready = state(null, 10_000, HealthConnectSyncState.Ready);
        var zero = new DailySteps(TODAY.toEpochDay(), 0, true);

        // When
        var unsupportedMessages = unsupported.stream()
                .map(sync -> state(null, 10_000, sync).todayEmptyMessage()).toList();
        var settingsMessages = unsupported.stream().map(SettingsScreen::statusMessage).toList();

        // Then
        assertEquals(List.of(R.string.no_step_total_today, R.string.no_step_total_today,
                R.string.no_step_total_today), unsupportedMessages);
        assertEquals(List.of(R.string.health_connect_unavailable, R.string.health_connect_update_required,
                R.string.health_connect_no_step_counter), settingsMessages);
        assertEquals(Integer.valueOf(R.string.no_steps_today), ready.todayEmptyMessage());
        assertNull(state(zero, 10_000, HealthConnectSyncState.Unavailable).todayEmptyMessage());
    }

    @Test
    public void givenActiveHomeState_whenTheSharedGoalChanges_thenHomeEmitsTheNewGoal() {
        // Given
        var fixture = new Fixture();
        var goals = new ArrayList<Integer>();
        fixture.home.uiState.observe(state -> goals.add(state.goal()));

        // When
        fixture.preferences.adjustGoal(100);

        // Then
        assertEquals(List.of(10_000, 10_100), goals);
    }

    @Test
    public void givenAnOpenScreen_whenBackupAndRefreshCommit_thenTotalsUpdateAndSurviveAFailedRefresh() {
        // Given
        var history = new History();
        var older = new DailySteps(TODAY.minusDays(40).toEpochDay(), 1_000, true);
        var backup = new DailySteps(TODAY.minusDays(1).toEpochDay(), 5_000, true);
        var fails = new AtomicBoolean();
        var fixture = new Fixture(history, withCacheRefresh(() -> {
            if (fails.get()) {
                throw new IllegalStateException("Health Connect read failed");
            }
            history.commit(List.of(older, backup, CURRENT));
        }));
        fixture.home.updateFirstDayOfWeek(MONDAY);
        history.commit(List.of(older));
        assertTrue(fixture.home.uiState.value().week().isEmpty());

        // When
        history.commit(List.of(older, backup));
        var imported = fixture.home.uiState.value();
        fixture.home.refresh(true);
        var refreshed = fixture.home.uiState.value();
        fails.set(true);
        fixture.home.refresh(true);
        var failed = fixture.home.uiState.value();

        // Then
        assertNull(imported.today());
        assertEquals(backup, imported.bestDay());
        assertEquals(Long.valueOf(6_000), imported.lifetimeSteps());
        assertEquals(HealthConnectSyncState.Ready, refreshed.sync());
        assertEquals(CURRENT, refreshed.today());
        assertEquals(CURRENT, refreshed.bestDay());
        assertEquals(Long.valueOf(16_000), refreshed.lifetimeSteps());
        assertEquals(List.of(backup, CURRENT), refreshed.week().days().stream()
                .map(WeekDay::total).filter(java.util.Objects::nonNull).toList());
        assertEquals(new HomeUiState(refreshed.date(), refreshed.week(), refreshed.today(), refreshed.goal(),
                HealthConnectSyncState.Error, refreshed.bestDay(), refreshed.lifetimeSteps()), failed);
    }

    @Test
    public void givenObservedTotals_whenLocalDateRollsOver_thenTodayBecomesTheNewestEntry() {
        // Given
        var fixture = new Fixture();
        var tomorrow = TODAY.plusDays(1);
        var newest = new DailySteps(tomorrow.toEpochDay(), 12_000, true);
        fixture.history.commit(List.of(CURRENT, newest));

        // When
        fixture.clock.set(tomorrow);
        fixture.home.refresh();

        // Then
        assertEquals(tomorrow, fixture.home.uiState.value().date());
        assertEquals(newest, fixture.home.uiState.value().today());
        assertEquals(Long.valueOf(22_000), fixture.home.uiState.value().lifetimeSteps());
    }

    @Test
    public void givenHomeObservers_whenBrowsingPreviousWeek_thenOnlyTheSelectedRangeQueryChanges() {
        // Given
        var fixture = new Fixture();
        fixture.home.updateFirstDayOfWeek(SUNDAY);
        fixture.history.ranges.clear();
        int historyReads = fixture.history.statsReads;

        // When
        fixture.home.previousWeek();

        // Then
        assertEquals(List.of(List.of(TODAY.minusWeeks(1), TODAY.minusDays(1))), fixture.history.ranges);
        assertEquals(historyReads, fixture.history.statsReads);
        assertEquals(TODAY, fixture.home.uiState.value().date());
    }

    @Test
    public void givenDailyTotals_whenBuildingLocaleWeek_thenBarsDistinguishZeroMissingAndUpcomingDays() {
        // Given
        var monday = TODAY.plusDays(1);
        var wednesday = monday.plusDays(2);
        var zero = new DailySteps(monday.toEpochDay(), 0, true);
        var recorded = new DailySteps(wednesday.toEpochDay(), 12_000, false);
        var future = new DailySteps(wednesday.plusDays(1).toEpochDay(), 5_000, true);
        var entries = List.of(CURRENT, zero, recorded, future);

        // When
        var mondayWeek = new WeekSelection().range(wednesday, MONDAY).withEntries(entries).days();
        var sundayWeek = new WeekSelection().range(wednesday, SUNDAY).withEntries(entries).days();

        // Then
        assertEquals(LongStream.rangeClosed(0, 6).mapToObj(monday::plusDays).toList(),
                mondayWeek.stream().map(WeekDay::date).toList());
        assertEquals(Arrays.asList(zero, null, recorded, null, null, null, null),
                mondayWeek.stream().map(WeekDay::total).toList());
        assertEquals(new WeekDay(TODAY, CURRENT), sundayWeek.get(0));
    }

    @Test
    public void givenSparseHistory_whenSummarizingCurrentAndPastWeeks_thenAveragesUseCalendarDays() {
        // Given
        var monday = TODAY.plusDays(1);
        var wednesday = monday.plusDays(2);
        var backup = new DailySteps(monday.toEpochDay(), 10_001, true);
        var zero = new DailySteps(wednesday.toEpochDay(), 0, false);
        var future = new DailySteps(wednesday.plusDays(1).toEpochDay(), 20_000, false);
        var entries = List.of(backup, zero, future);
        var selection = new WeekSelection();

        // When
        var firstDay = selection.range(monday, MONDAY).withEntries(entries);
        var midweek = selection.range(wednesday, MONDAY).withEntries(entries);
        var next = monday.plusWeeks(1);
        var past = selection.previous(next, MONDAY).range(next, MONDAY).withEntries(entries);

        // Then
        assertEquals(10_001, firstDay.average());
        assertEquals(10_001, midweek.total());
        assertEquals(3_333, midweek.average());
        assertEquals(backup, midweek.days().get(0).total());
        assertEquals(30_001, past.total());
        assertEquals(4_285, past.average());
        assertEquals(12_000, midweek.scaleMaximum(12_000));
        assertEquals(20_000, past.scaleMaximum(12_000));
    }

    @Test
    public void givenCurrentWeek_whenBrowsingAcrossNewYear_thenNavigationStopsOnReturnToCurrent() {
        // Given
        var january = LocalDate.of(2026, 1, 4);
        var current = new WeekSelection();

        // When
        var past = current.previous(january, MONDAY);
        var earlier = past.previous(january, MONDAY);
        var returned = earlier.next(january, MONDAY).next(january, MONDAY);
        var rollover = january.plusDays(1);

        // Then
        assertEquals(LocalDate.of(2025, 12, 29), current.start(january, MONDAY));
        assertEquals(january, current.start(january, SUNDAY));
        assertEquals(LocalDate.of(2025, 12, 15), earlier.start(january, MONDAY));
        assertTrue(earlier.range(january, MONDAY).canGoNext());
        assertEquals(current, returned);
        assertFalse(returned.range(january, MONDAY).canGoNext());
        assertEquals(current, returned.next(january, MONDAY));
        assertEquals(rollover, returned.start(rollover, MONDAY));
        assertEquals(past.start(january, MONDAY), past.start(rollover, MONDAY));
    }

    @Test
    public void givenMissingHistoryAndRecordedZero_whenSelectingWeeks_thenOnlyMissingHistoryIsEmpty() {
        // Given
        var zero = new DailySteps(TODAY.toEpochDay(), 0, false);
        var current = new WeekSelection();

        // When
        var recorded = current.range(TODAY, SUNDAY).withEntries(List.of(zero));
        var empty = current.previous(TODAY, SUNDAY).range(TODAY, SUNDAY).withEntries(List.of());

        // Then
        assertFalse(recorded.isEmpty());
        assertEquals(zero, recorded.days().get(0).total());
        assertNull(recorded.days().get(1).total());
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.total());
        assertEquals(0, empty.average());
    }

    @Test
    public void givenASavedGoal_whenComparingTotals_thenEqualityReachesTheGoal() {
        // Given
        var atGoal = state(CURRENT, 10_000, HealthConnectSyncState.Ready);
        var below = state(CURRENT, 10_001, HealthConnectSyncState.Ready);

        // When
        var reached = atGoal.reachedGoal(CURRENT.steps());

        // Then
        assertTrue(reached);
        assertFalse(below.reachedGoal(CURRENT.steps()));
        assertEquals(1f, atGoal.progress(), 0f);
        assertEquals(1f, state(CURRENT, 1, HealthConnectSyncState.Ready).progress(), 0f);
    }

    @Test
    public void givenAPartialDay_whenComparingStepsToGoal_thenProgressIsFractional() {
        // Given
        var entry = new DailySteps(TODAY.toEpochDay(), 8_844, false);

        // When
        var state = state(entry, 10_000, HealthConnectSyncState.Ready);

        // Then
        assertEquals(0.8844f, state.progress(), 0.00001f);
        assertFalse(state.reachedGoal(entry.steps()));
    }

    @Test
    public void givenAMissingDayAndBackupZero_whenReadingProgress_thenOnlyZeroHasATotal() {
        // Given
        var entry = new DailySteps(TODAY.toEpochDay(), 0, true);

        // When
        var missing = state(null, 10_000, HealthConnectSyncState.Loading);
        var recorded = state(entry, 10_000, HealthConnectSyncState.Loading);

        // Then
        assertEquals(0f, missing.progress(), 0f);
        assertEquals(0f, recorded.progress(), 0f);
        assertNull(missing.today());
        assertEquals(entry, recorded.today());
    }

    @Test
    public void givenAGoalOffTheHundredGrid_whenAdjustedEitherWay_thenItPreservesItsOffset() {
        // Given
        int goal = 7_550;

        // When
        var adjusted = Stream.of(-100, 100).map(amount -> NemiPreferences.adjustGoal(goal, amount)).toList();

        // Then
        assertEquals(List.of(7_450, 7_650), adjusted);
    }

    @Test
    public void givenGoalsNearOrAtLimits_whenAdjustedOutward_thenTheyClamp() {
        // Given
        var lower = List.of(1, 50);
        var upper = List.of(99_950, 100_000);

        // When
        var lowerResults = lower.stream().map(goal -> NemiPreferences.adjustGoal(goal, -100)).toList();
        var upperResults = upper.stream().map(goal -> NemiPreferences.adjustGoal(goal, 100)).toList();

        // Then
        assertEquals(List.of(1, 1), lowerResults);
        assertEquals(List.of(100_000, 100_000), upperResults);
    }

    @Test
    public void givenAHeldGoalButton_whenSuccessiveRepeatsOccur_thenDelayDecreasesToMinimum() {
        // Given
        long start = SettingsScreen.FIRST_GOAL_REPEAT_DELAY;

        // When
        var delays = Stream.iterate(start, SettingsScreen::nextGoalRepeatDelay).limit(10).toList();

        // Then
        assertEquals(List.of(150L, 135L, 120L, 105L, 90L, 75L, 60L, 45L, 45L, 45L), delays);
    }

    private static HomeUiState state(DailySteps entry, int goal, HealthConnectSyncState sync) {
        var week = new WeekSelection().range(TODAY, SUNDAY).withEntries(entry == null ? List.of() : List.of(entry));
        return new HomeUiState(TODAY, week, entry, goal, sync, null, null);
    }

    private static final class Fixture {
        final AtomicReference<LocalDate> clock = new AtomicReference<>(TODAY);
        final History history;
        final NemiPreferences preferences = new NemiPreferences(
                NemiPreferencesTest.inMemoryPreferences(new HashMap<>()));
        final HomeViewModel home;

        Fixture() {
            this(new History(), withoutPermission());
        }

        Fixture(History history, HealthConnectSyncCoordinator coordinator) {
            this.history = history;
            home = new HomeViewModel(preferences, history, coordinator, Runnable::run, Runnable::run, clock::get);
        }
    }

    private static final class History implements HomeViewModel.StepsHistory {
        final List<List<LocalDate>> ranges = new ArrayList<>();
        private List<DailySteps> entries = List.of();
        private Runnable observer;
        int statsReads;

        @Override public Runnable observeChanges(Runnable listener) {
            observer = listener;
            observer.run();
            return () -> observer = null;
        }

        @Override public List<DailySteps> readDays(LocalDate start, LocalDate end) {
            ranges.add(List.of(start, end));
            return entries.stream()
                    .filter(day -> day.day() >= start.toEpochDay() && day.day() <= end.toEpochDay())
                    .toList();
        }

        @Override public StepHistoryStats historyStats() {
            statsReads++;
            return new StepHistoryStats(entries.stream().mapToLong(DailySteps::steps).sum(),
                    entries.stream().max(java.util.Comparator.comparingLong(DailySteps::steps)).orElse(null));
        }

        void commit(List<DailySteps> next) {
            entries = next;
            observer.run();
        }
    }
}
