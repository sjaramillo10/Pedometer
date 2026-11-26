package dev.sjaramillo.nemi.data;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import java.time.LocalDate;

import java.util.List;

public class StepsCsvTest {
    @Test
    public void givenASingleDigitMonthAndDay_whenNamingAnExport_thenIncludesThePaddedLocalDate() {
        // Given
        var date = LocalDate.of(2026, 2, 3);

        // When
        var filename = StepsCsv.fileName(date);

        // Then
        assertEquals("Nemi_2026-02-03.csv", filename);
    }

    @Test
    public void givenUnsupportedEpochDays_whenImporting_thenIgnoresThemAndKeepsValidBoundaryDates() {
        // Given
        var lastDay = LocalDate.MAX.toEpochDay();
        var csv = "1,100\n0,200\n" + lastDay + ",300\n" + (lastDay + 1) + ",400\n1795392000000,1234\n";

        // When
        var imported = StepsCsv.decode(csv);

        // Then
        assertEquals(
            List.of(new DailySteps(1, 100, true), new DailySteps(lastDay, 300, true)),
            imported.dailySteps()
        );
        assertEquals(3, imported.ignoredRows());
    }

    @Test
    public void givenUnsortedTotalsFromBothSources_whenExporting_thenWritesTwoColumnsInAscendingDayOrder() {
        // Given
        var totals = List.of(new DailySteps(20, 200, true), new DailySteps(10, 100, false));

        // When
        var exported = StepsCsv.encode(totals);

        // Then
        assertEquals("10,100\n20,200\n", exported);
    }

    @Test
    public void givenValidAndInvalidCSVRows_whenImporting_thenMarksValidRowsAsBackupTotals() {
        // Given
        var csv = "20,200\ninvalid\n10,-1\n30,300\n";

        // When
        var imported = StepsCsv.decode(csv);

        // Then
        assertEquals(
            List.of(new DailySteps(20, 200, true), new DailySteps(30, 300, true)),
            imported.dailySteps()
        );
        assertEquals(2, imported.ignoredRows());
    }
}
