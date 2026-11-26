package dev.sjaramillo.nemi.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Test;

public class BackupWorkTest {
    @Test
    public void givenPendingProviderRead_whenHistoryWorkRuns_thenItDoesNotWaitForTheProvider() {
        // Given
        var documents = new ArrayDeque<Runnable>();
        var database = new ArrayDeque<Runnable>();
        var backup = new BackupWork(database::add, documents::add);
        var result = backup.importFile(() -> "CSV", value -> value + " imported");

        // When
        var historyLoaded = new AtomicBoolean();
        database.add(() -> historyLoaded.set(true));
        database.remove().run();

        // Then
        assertTrue(historyLoaded.get());
        assertFalse(result.isDone());
        documents.remove().run();
        database.remove().run();
        assertEquals("CSV imported", result.join());
    }

    @Test
    public void givenImportWaitingToWrite_whenModelCloses_thenDatabaseWriteIsSkipped() {
        // Given
        var database = new ArrayDeque<Runnable>();
        var backup = new BackupWork(database::add, Runnable::run);
        var wrote = new AtomicBoolean();
        var result = backup.importFile(() -> "CSV", value -> {
            wrote.set(true);
            return value;
        });

        // When
        backup.close();
        database.remove().run();

        // Then
        assertFalse(wrote.get());
        assertTrue(result.isCompletedExceptionally());
    }

    @Test
    public void givenImportReadingFromTheProvider_whenModelCloses_thenItCannotWrite() {
        // Given
        var backup = new BackupWork(Runnable::run, Runnable::run);
        var wrote = new AtomicBoolean();

        // When
        var result = backup.importFile(() -> {
            backup.close();
            return "CSV";
        }, value -> {
            wrote.set(true);
            return value;
        });

        // Then
        assertFalse(wrote.get());
        assertTrue(result.isCompletedExceptionally());
    }
}
