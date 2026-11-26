package dev.sjaramillo.nemi;

import android.app.Application;
import dev.sjaramillo.nemi.data.HealthConnectSyncCoordinator;
import dev.sjaramillo.nemi.data.NemiDatabase;
import dev.sjaramillo.nemi.data.PhoneStepsDataSource;
import dev.sjaramillo.nemi.data.StepsRepository;
import dev.sjaramillo.nemi.ui.HomeViewModel;
import dev.sjaramillo.nemi.ui.NemiPreferences;
import dev.sjaramillo.nemi.ui.SettingsViewModel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class NemiApplication extends Application {
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private StepsRepository steps;
    private NemiPreferences preferences;
    private HealthConnectSyncCoordinator healthConnect;

    @Override public void onCreate() {
        super.onCreate();
        var main = getMainExecutor();
        steps = new StepsRepository(new NemiDatabase(this), main);
        preferences = new NemiPreferences(getSharedPreferences("nemi", MODE_PRIVATE));
        healthConnect = new HealthConnectSyncCoordinator(new PhoneStepsDataSource(this), steps, worker, main);
    }

    public HomeViewModel createHomeViewModel() {
        return new HomeViewModel(preferences, steps, healthConnect, worker, getMainExecutor());
    }

    public SettingsViewModel createSettingsViewModel() {
        return new SettingsViewModel(this, preferences, steps, healthConnect, worker);
    }
}
