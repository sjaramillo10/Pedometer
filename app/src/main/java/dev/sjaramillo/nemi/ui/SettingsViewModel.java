package dev.sjaramillo.nemi.ui;

import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import dev.sjaramillo.nemi.R;
import dev.sjaramillo.nemi.State;
import dev.sjaramillo.nemi.data.HealthConnectSyncCoordinator;
import dev.sjaramillo.nemi.data.HealthConnectSyncState;
import dev.sjaramillo.nemi.data.StepsCsv;
import dev.sjaramillo.nemi.data.StepsRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executor;

public final class SettingsViewModel {
    private final Context context;
    private final NemiPreferences preferences;
    private final StepsRepository steps;
    private final HealthConnectSyncCoordinator coordinator;
    private final Executor worker;
    private final String version;
    private final Runnable stopGoal;
    private final Runnable stopTheme;
    private final Runnable stopSync;
    private FileState file = new FileState(false, null);
    private boolean closed;
    final State<SettingsUiState> uiState;

    public SettingsViewModel(Context context, NemiPreferences preferences, StepsRepository steps,
            HealthConnectSyncCoordinator coordinator, Executor worker) {
        this.context = context;
        this.preferences = preferences;
        this.steps = steps;
        this.coordinator = coordinator;
        this.worker = worker;
        version = readVersion(context);
        uiState = new State<>(snapshot());
        stopGoal = preferences.goal.observe(ignored -> publish());
        stopTheme = preferences.theme.observe(ignored -> publish());
        stopSync = coordinator.observe(ignored -> publish());
    }

    void adjustGoal(int amount) {
        preferences.adjustGoal(amount);
    }

    void saveTheme(AppTheme theme) {
        preferences.setTheme(theme);
    }

    void importCsv(Uri uri) {
        runFileOperation(() -> {
            StepsCsv.Import decoded;
            try (var input = context.getContentResolver().openInputStream(uri)) {
                if (input == null) {
                    throw new IOException("Unable to open import source");
                }
                decoded = StepsCsv.decode(new String(input.readAllBytes(), StandardCharsets.UTF_8));
            }
            steps.importDailySteps(decoded.dailySteps());
            return context.getString(R.string.entries_imported, decoded.dailySteps().size(),
                    decoded.ignoredRows());
        });
    }

    void exportCsv(Uri uri) {
        runFileOperation(() -> {
            String csv = StepsCsv.encode(steps.getAll());
            try (var output = context.getContentResolver().openOutputStream(uri)) {
                if (output == null) {
                    throw new IOException("Unable to open export destination");
                }
                output.write(csv.getBytes(StandardCharsets.UTF_8));
            }
            return context.getString(R.string.data_exported);
        });
    }

    void dismissFileMessage() {
        file = new FileState(file.busy(), null);
        publish();
    }

    void close() {
        closed = true;
        stopGoal.run();
        stopTheme.run();
        stopSync.run();
    }

    private void runFileOperation(FileOperation operation) {
        if (file.busy()) {
            return;
        }
        file = new FileState(true, null);
        publish();
        worker.execute(() -> {
            String message;
            try {
                message = operation.run();
            } catch (Exception error) {
                String detail = error.getLocalizedMessage();
                message = context.getString(R.string.error_file, detail == null ? "" : detail);
            }
            String result = message;
            context.getMainExecutor().execute(() -> {
                if (!closed) {
                    file = new FileState(false, result);
                    publish();
                }
            });
        });
    }

    private void publish() {
        uiState.set(snapshot());
    }

    private SettingsUiState snapshot() {
        return new SettingsUiState(preferences.goal.value(), preferences.theme.value(), file,
                version, coordinator.state());
    }

    private static String readVersion(Context context) {
        try {
            String version = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            return version == null ? "" : version;
        } catch (NameNotFoundException error) {
            throw new IllegalStateException(error);
        }
    }

    private interface FileOperation {
        String run() throws Exception;
    }
}

record FileState(boolean busy, String message) {}

record SettingsUiState(int goal, AppTheme theme, FileState file, String version,
        HealthConnectSyncState sync) {}
