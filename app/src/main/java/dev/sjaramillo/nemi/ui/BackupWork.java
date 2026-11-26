package dev.sjaramillo.nemi.ui;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

final class BackupWork {
    private final Executor database;
    private final Executor documents;
    private volatile boolean closed;

    BackupWork(Executor database, Executor documents) {
        this.database = database;
        this.documents = documents;
    }

    <T> CompletableFuture<String> importFile(Callable<T> read, BackupWrite<T> write) {
        return transfer(documents, read, database, write);
    }

    <T> CompletableFuture<String> exportFile(Callable<T> read, BackupWrite<T> write) {
        return transfer(database, read, documents, write);
    }

    void close() {
        closed = true;
    }

    private <T> CompletableFuture<String> transfer(Executor reader, Callable<T> read,
            Executor writer, BackupWrite<T> write) {
        return CompletableFuture.supplyAsync(() -> runIfOpen(read), reader)
                .thenApplyAsync(value -> runIfOpen(() -> write.run(value)), writer);
    }

    private <T> T runIfOpen(Callable<T> operation) {
        // The volatile read admits an operation. Close prevents later admissions.
        if (closed) {
            throw new CancellationException();
        }
        try {
            return operation.call();
        } catch (Exception error) {
            throw new CompletionException(error);
        }
    }

    interface BackupWrite<T> {
        String run(T value) throws Exception;
    }
}
