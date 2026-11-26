package dev.sjaramillo.nemi.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import dev.sjaramillo.nemi.NemiApplication;

public final class MainActivity extends Activity {
    private NemiRoute route;
    private Models models;

    @Override @SuppressWarnings("deprecation")
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        var window = getWindow();
        window.setDecorFitsSystemWindows(false);
        var attributes = window.getAttributes();
        attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        window.setAttributes(attributes);
        if (Build.VERSION.SDK_INT < 35) {
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }
        window.setStatusBarContrastEnforced(false);
        window.setNavigationBarContrastEnforced(false);
        var app = (NemiApplication) getApplication();
        models = getLastNonConfigurationInstance() instanceof Models retained
                ? retained : new Models(app.createHomeViewModel(), app.createSettingsViewModel());
        route = new NemiRoute(this, saved, models.home(), models.settings());
    }

    @Override protected void onStart() {
        super.onStart();
        route.start();
    }

    @Override protected void onResume() {
        super.onResume();
        route.resume();
    }

    @Override protected void onStop() {
        route.stop();
        super.onStop();
    }

    @Override protected void onDestroy() {
        route.destroy();
        if (!isChangingConfigurations()) {
            models.home().close();
            models.settings().close();
        }
        super.onDestroy();
    }

    @Override @SuppressWarnings("deprecation")
    public Object onRetainNonConfigurationInstance() {
        return models;
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        route.handleIntent(intent);
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        route.saveState(out);
    }

    @Override @SuppressWarnings("deprecation")
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        route.onDocumentResult(requestCode, resultCode, data == null ? null : data.getData());
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
            int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        route.onPermissionResult(requestCode);
    }

    private record Models(HomeViewModel home, SettingsViewModel settings) {}
}
