package dev.sjaramillo.nemi.data;

import java.time.LocalDate;

public record DailySteps(long day, long steps, boolean fromBackup) {
    static final long LAST_SUPPORTED_EPOCH_DAY = LocalDate.MAX.toEpochDay();

    static boolean isSupportedDay(long day) {
        return day >= 1 && day <= LAST_SUPPORTED_EPOCH_DAY;
    }
}
