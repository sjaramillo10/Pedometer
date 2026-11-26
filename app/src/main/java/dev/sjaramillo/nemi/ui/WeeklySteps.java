package dev.sjaramillo.nemi.ui;

import dev.sjaramillo.nemi.data.DailySteps;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

record WeeklySteps(List<WeekDay> days, WeekRange range) {
    boolean isEmpty() {
        return days.stream().noneMatch(day -> day.total() != null);
    }

    long total() {
        return days.stream().mapToLong(WeekDay::steps).sum();
    }

    long average() {
        long count = days.stream().filter(day -> !day.date().isAfter(range.through())).count();
        return total() / Math.max(count, 1);
    }

    long scaleMaximum(int goal) {
        return Math.max(goal, days.stream().mapToLong(WeekDay::steps).max().orElse(0));
    }
}

record WeekDay(LocalDate date, DailySteps total) {
    long steps() {
        return total == null ? 0 : total.steps();
    }
}

record WeekRange(LocalDate start, LocalDate through, boolean canGoNext) {
    WeeklySteps withEntries(List<DailySteps> entries) {
        var byDay = new HashMap<Long, DailySteps>();
        for (DailySteps entry : entries) {
            byDay.put(entry.day(), entry);
        }
        var days = new ArrayList<WeekDay>(7);
        for (int index = 0; index < 7; index++) {
            LocalDate day = start.plusDays(index);
            days.add(new WeekDay(day, day.isAfter(through) ? null : byDay.get(day.toEpochDay())));
        }
        return new WeeklySteps(List.copyOf(days), this);
    }
}

record WeekSelection(LocalDate anchor) {
    WeekSelection() {
        this(null);
    }

    LocalDate start(LocalDate today, DayOfWeek firstDay) {
        LocalDate day = anchor == null || anchor.isAfter(today) ? today : anchor;
        return day.with(TemporalAdjusters.previousOrSame(firstDay));
    }

    WeekSelection previous(LocalDate today, DayOfWeek firstDay) {
        return new WeekSelection(start(today, firstDay).minusWeeks(1));
    }

    WeekSelection next(LocalDate today, DayOfWeek firstDay) {
        LocalDate next = start(today, firstDay).plusWeeks(1);
        return next.isBefore(new WeekSelection().start(today, firstDay))
                ? new WeekSelection(next) : new WeekSelection();
    }

    WeekRange range(LocalDate today, DayOfWeek firstDay) {
        LocalDate start = start(today, firstDay);
        LocalDate end = start.plusDays(6);
        return new WeekRange(start, end.isAfter(today) ? today : end,
                start.isBefore(new WeekSelection().start(today, firstDay)));
    }
}
