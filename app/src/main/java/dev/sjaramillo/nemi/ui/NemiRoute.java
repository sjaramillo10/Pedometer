package dev.sjaramillo.nemi.ui;

import static android.health.connect.HealthPermissions.READ_STEPS;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowInsetsController;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import dev.sjaramillo.nemi.R;
import dev.sjaramillo.nemi.data.StepsCsv;
import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.Objects;

final class NemiRoute {
    private static final int READ_PERMISSION = 1;
    private static final int IMPORT_DOCUMENT = 2;
    private static final int EXPORT_DOCUMENT = 3;

    private final Activity activity;
    private final HomeViewModel home;
    private final SettingsViewModel settings;
    private final OnBackInvokedCallback settingsBack = this::closeSettings;
    private final BroadcastReceiver dateChanges = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            home.refresh();
        }
    };
    private boolean settingsBackRegistered;
    private boolean showSettings;
    private boolean showPrivacy;
    private boolean revealHealthConnect;
    private int homeScroll;
    private int settingsScroll;
    private HomeScreen homeScreen;
    private SettingsScreen settingsScreen;
    private Palette palette;
    private Dialog dialog;
    private DialogContent dialogContent;
    private Runnable stopHome;
    private Runnable stopSettings;

    NemiRoute(Activity activity, Bundle saved, HomeViewModel home, SettingsViewModel settings) {
        this.activity = activity;
        this.home = home;
        this.settings = settings;
        if (saved != null) {
            showSettings = saved.getBoolean("settings");
            showPrivacy = saved.getBoolean("privacy");
            revealHealthConnect = saved.getBoolean("revealHealthConnect");
            homeScroll = saved.getInt("homeScroll");
            settingsScroll = saved.getInt("settingsScroll");
        } else {
            showPrivacy = opensPrivacyPolicy(activity.getIntent());
        }
        home.updateFirstDayOfWeek(WeekFields.of(activity.getResources().getConfiguration()
                .getLocales().get(0)).getFirstDayOfWeek());
        render();
    }

    void start() {
        stopHome = home.uiState.observe(ignored -> render());
        stopSettings = settings.uiState.observe(ignored -> render());
        var filter = new IntentFilter();
        filter.addAction(Intent.ACTION_DATE_CHANGED);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        activity.registerReceiver(dateChanges, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    void resume() {
        home.refresh();
    }

    void stop() {
        stopHome.run();
        stopSettings.run();
        activity.unregisterReceiver(dateChanges);
    }

    void destroy() {
        setSettingsBackRegistered(false);
        dismissDialog();
    }

    void onPermissionResult(int requestCode) {
        if (requestCode == READ_PERMISSION) {
            home.refresh(true);
        }
    }

    void onDocumentResult(int requestCode, int resultCode, Uri uri) {
        if (resultCode != Activity.RESULT_OK || uri == null) {
            return;
        }
        switch (requestCode) {
            case IMPORT_DOCUMENT -> settings.importCsv(uri);
            case EXPORT_DOCUMENT -> settings.exportCsv(uri);
        }
    }

    void handleIntent(Intent intent) {
        showPrivacy = opensPrivacyPolicy(intent);
        render();
    }

    void saveState(Bundle out) {
        rememberScroll();
        out.putBoolean("settings", showSettings);
        out.putBoolean("privacy", showPrivacy);
        out.putBoolean("revealHealthConnect", revealHealthConnect);
        out.putInt("homeScroll", homeScroll);
        out.putInt("settingsScroll", settingsScroll);
    }

    @SuppressWarnings("deprecation")
    private void launchDocumentPicker(String action, String mimeType, int requestCode, String title) {
        var intent = new Intent(action).addCategory(Intent.CATEGORY_OPENABLE).setType(mimeType);
        if (title != null) {
            intent.putExtra(Intent.EXTRA_TITLE, title);
        }
        activity.startActivityForResult(intent, requestCode);
    }

    private void setSettingsBackRegistered(boolean registered) {
        if (settingsBackRegistered == registered) {
            return;
        }
        var dispatcher = activity.getOnBackInvokedDispatcher();
        if (registered) {
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, settingsBack);
        } else {
            dispatcher.unregisterOnBackInvokedCallback(settingsBack);
        }
        settingsBackRegistered = registered;
    }

    private boolean opensPrivacyPolicy(Intent intent) {
        var component = intent.getComponent();
        return Intent.ACTION_VIEW_PERMISSION_USAGE.equals(intent.getAction()) && component != null
                && (activity.getPackageName() + ".ViewHealthConnectPermissionUsageActivity")
                        .equals(component.getClassName());
    }

    private void openSettings(boolean revealHealth) {
        showSettings = true;
        revealHealthConnect = revealHealth;
        render();
    }

    private void closeSettings() {
        showSettings = false;
        revealHealthConnect = false;
        settings.dismissFileMessage();
        render();
        settingsScroll = 0;
    }

    private void rememberScroll() {
        if (homeScreen != null) {
            homeScroll = homeScreen.scroll().getScrollY();
        }
        if (settingsScreen != null) {
            settingsScroll = settingsScreen.scroll().getScrollY();
        }
    }

    private void render() {
        HomeUiState homeState = home.uiState.value();
        SettingsUiState settingsState = settings.uiState.value();
        int mode = activity.getResources().getConfiguration().uiMode;
        boolean dark = settingsState.theme().isDark(
                (mode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES);
        Palette colors = dark ? Palette.DARK : Palette.LIGHT;
        boolean themeChanged = palette != colors;
        if (themeChanged) {
            var controller = activity.getWindow().getDecorView().getWindowInsetsController();
            if (controller != null) {
                int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                controller.setSystemBarsAppearance(dark ? 0 : lightBars, lightBars);
            }
            palette = colors;
        }
        setSettingsBackRegistered(showSettings);
        if (showSettings) {
            renderSettings(settingsState, colors, themeChanged);
        } else {
            if (homeScreen == null || themeChanged) {
                rememberScroll();
                settingsScreen = null;
                homeScreen = new HomeScreen(activity, colors, this::openSettings,
                        home::previousWeek, home::nextWeek);
                homeScreen.bind(homeState);
                activity.setContentView(homeScreen.root());
                homeScreen.restoreScroll(homeScroll);
            } else {
                homeScreen.bind(homeState);
            }
        }
        updateDialog(settingsState, colors, themeChanged);
    }

    private void renderSettings(SettingsUiState state, Palette colors, boolean themeChanged) {
        if (settingsScreen == null || themeChanged) {
            rememberScroll();
            homeScreen = null;
            settingsScreen = new SettingsScreen(activity, colors, this::closeSettings,
                    () -> activity.requestPermissions(new String[] {READ_STEPS}, READ_PERMISSION),
                    () -> home.refresh(true), settings::adjustGoal, settings::saveTheme,
                    () -> launchDocumentPicker(Intent.ACTION_OPEN_DOCUMENT, "text/*", IMPORT_DOCUMENT, null),
                    () -> launchDocumentPicker(Intent.ACTION_CREATE_DOCUMENT, "text/csv",
                            EXPORT_DOCUMENT, StepsCsv.fileName(LocalDate.now())),
                    settings::dismissFileMessage);
            settingsScreen.bind(state);
            activity.setContentView(settingsScreen.root());
            settingsScreen.restoreScroll(settingsScroll);
        } else {
            settingsScreen.bind(state);
        }
        if (revealHealthConnect) {
            revealHealthConnect = false;
            settingsScreen.revealHealthConnect();
        }
    }

    private void updateDialog(SettingsUiState state, Palette colors, boolean themeChanged) {
        DialogContent content = null;
        if (showPrivacy) {
            content = new DialogContent(R.string.privacy_title,
                    activity.getString(R.string.health_connect_privacy_policy));
        } else if (!showSettings && state.file().message() != null) {
            content = new DialogContent(R.string.backup_result, state.file().message());
        }
        if (Objects.equals(content, dialogContent) && !themeChanged) {
            return;
        }
        dismissDialog();
        dialogContent = content;
        if (content != null) {
            int title = content.title();
            dialog = AppComponents.showDialog(activity, colors, title, content.message(), () -> {
                if (title == R.string.privacy_title) {
                    showPrivacy = false;
                } else {
                    settings.dismissFileMessage();
                }
                render();
            });
        }
    }

    private void dismissDialog() {
        if (dialog != null) {
            dialog.setOnDismissListener(null);
            dialog.dismiss();
            dialog = null;
        }
    }

    private record DialogContent(int title, String message) {}
}
