package dev.sjaramillo.nemi.ui;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static dev.sjaramillo.nemi.ui.AppComponents.*;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import dev.sjaramillo.nemi.R;
import dev.sjaramillo.nemi.data.HealthConnectSyncState;
import java.util.EnumMap;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

final class SettingsScreen {
    static final long FIRST_GOAL_REPEAT_DELAY = 150;
    private final Context context;
    private final Palette colors;
    private final Runnable onConnect;
    private final Runnable onRetry;
    private final ScreenLayout screen;
    private final TextView goal;
    private final Button decrease;
    private final Button increase;
    private final EnumMap<AppTheme, RadioButton> themeOptions = new EnumMap<>(AppTheme.class);
    private final LinearLayout healthStatus;
    private final View importRow;
    private final View exportRow;
    private final TextView working;
    private final TextView result;
    private final Button dismiss;
    private final TextView version;
    private final LinearLayout healthCard;
    private SettingsUiState previous;

    SettingsScreen(Context context, Palette colors, Runnable onBack, Runnable onConnect,
            Runnable onRetry, IntConsumer onAdjustGoal, Consumer<AppTheme> onSetTheme,
            Runnable onImport, Runnable onExport, Runnable onDismissFileMessage) {
        this.context = context;
        this.colors = colors;
        this.onConnect = onConnect;
        this.onRetry = onRetry;
        screen = new ScreenLayout(context, colors, R.string.settings_title, R.drawable.ic_back,
                R.string.back, true, onBack);
        goal = label(context, "", colors.text(), 24, 600, true);
        decrease = goalButton(-100, onAdjustGoal);
        increase = goalButton(100, onAdjustGoal);
        healthStatus = column(context);
        importRow = backupRow(R.string.import_title, R.string.import_summary, onImport);
        exportRow = backupRow(R.string.export_title, R.string.export_summary, onExport);
        working = label(context, context.getString(R.string.backup_working), colors.text());
        liveRegion(working);
        result = label(context, "", colors.text());
        liveRegion(result);
        dismiss = action(context, context.getString(R.string.dismiss_result), colors, onDismissFileMessage);
        version = label(context, "", colors.secondary(), 13, 400, true);

        var goalCard = card(context, R.string.daily_step_goal, colors);
        var goalRow = row(context);
        goalRow.setBackground(rounded(context, colors.track(), 16));
        pad(goalRow, 8);
        goalRow.addView(decrease);
        var goalLabels = column(context);
        liveRegion(goalLabels);
        pad(goalLabels, 8, 0);
        item(goalLabels, goal);
        item(goalLabels, label(context, context.getString(R.string.steps_per_day), colors.secondary(), 14, 400, true));
        weighted(goalRow, goalLabels);
        goalRow.addView(increase);
        item(goalCard, goalRow);
        item(screen.content, goalCard);

        var appearance = card(context, R.string.appearance, colors);
        var group = new RadioGroup(context);
        for (AppTheme theme : AppTheme.values()) {
            if (!themeOptions.isEmpty()) {
                group.addView(divider(context, colors), new RadioGroup.LayoutParams(MATCH_PARENT, dp(context, 1)));
            }
            var button = themeOption(theme, onSetTheme);
            themeOptions.put(theme, button);
            group.addView(button, new RadioGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT));
        }
        item(appearance, group);
        item(screen.content, appearance, 16);

        healthCard = card(context, R.string.health_connect, colors);
        item(healthCard, label(context, context.getString(R.string.health_connect_description), colors.secondary(), 14));
        item(healthCard, healthStatus, 12);
        item(healthCard, link(R.string.learn_health_connect, "https://developer.android.com/health-and-fitness/health-connect/get-started", "›"));
        item(screen.content, healthCard, 16);

        var backup = card(context, R.string.backup, colors);
        item(backup, importRow);
        item(backup, divider(context, colors), 0, dp(context, 1));
        item(backup, exportRow);
        item(backup, working, 6);
        item(backup, result, 6);
        item(backup, dismiss);
        item(screen.content, backup, 16);

        var about = card(context, R.string.about, colors);
        item(about, version);
        var text = label(context, Html.fromHtml(context.getString(R.string.about_text),
                Html.FROM_HTML_MODE_COMPACT), colors.text(), 14);
        text.setMovementMethod(LinkMovementMethod.getInstance());
        text.setLinkTextColor(colors.accent());
        item(about, text, 16);
        item(about, divider(context, colors), 10, dp(context, 1));
        item(about, link(R.string.source_code, "https://github.com/sjaramillo10/Nemi", "↗"));
        item(screen.content, about, 16);
    }

    void bind(SettingsUiState state) {
        SettingsUiState old = previous;
        if (state.equals(old)) {
            return;
        }
        previous = state;
        if (old == null || old.goal() != state.goal()) {
            goal.setText(context.getString(R.string.selected_goal, state.goal()));
            decrease.setEnabled(NemiPreferences.adjustGoal(state.goal(), -100) != state.goal());
            increase.setEnabled(NemiPreferences.adjustGoal(state.goal(), 100) != state.goal());
        }
        if (old == null || old.theme() != state.theme()) {
            themeOptions.forEach((theme, button) -> {
                boolean selected = theme == state.theme();
                button.setChecked(selected);
                int tint = selected ? colors.accent() : withAlpha(colors.text(), 0.40f);
                var ring = new GradientDrawable();
                ring.setShape(GradientDrawable.OVAL);
                ring.setStroke(dp(context, selected ? 7 : 1), tint);
                ring.setBounds(0, 0, dp(context, 20), dp(context, 20));
                button.setCompoundDrawablesRelative(null, null, ring, null);
            });
        }
        if (old == null || old.sync() != state.sync()) {
            bindHealthStatus(state.sync());
        }
        if (old == null || !old.version().equals(state.version())) {
            version.setText(context.getString(R.string.about_app_version, state.version()));
        }
        if (old == null || !old.file().equals(state.file())) {
            importRow.setEnabled(!state.file().busy());
            exportRow.setEnabled(!state.file().busy());
            working.setVisibility(state.file().busy() ? View.VISIBLE : View.GONE);
            result.setText(state.file().message());
            result.setVisibility(state.file().message() == null ? View.GONE : View.VISIBLE);
            dismiss.setVisibility(result.getVisibility());
        }
    }

    View root() {
        return screen.root;
    }

    ScrollView scroll() {
        return screen.scroll;
    }

    void restoreScroll(int position) {
        screen.restoreScroll(position);
    }

    void revealHealthConnect() {
        var scroll = screen.scroll;
        scroll.post(() -> {
            if (scroll.isLaidOut() && !scroll.isLayoutRequested()) {
                scrollToHealthConnect();
            } else {
                scroll.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                    @Override public void onLayoutChange(View view, int left, int top, int right,
                            int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                        view.removeOnLayoutChangeListener(this);
                        scrollToHealthConnect();
                    }
                });
            }
        });
    }

    private void scrollToHealthConnect() {
        var bounds = new Rect();
        screen.scroll.offsetDescendantRectToMyCoords(healthCard, bounds);
        screen.scroll.smoothScrollTo(0, bounds.top - screen.scroll.getPaddingTop());
    }

    private void bindHealthStatus(HealthConnectSyncState status) {
        healthStatus.removeAllViews();
        String message = context.getString(statusMessage(status));
        boolean connected = status == HealthConnectSyncState.Ready;
        boolean showBadge = connected || status == HealthConnectSyncState.PermissionRequired;
        var row = row(context);
        liveRegion(row);
        row.setBackground(rounded(context, colors.background(), 16));
        pad(row, 12);
        var dot = new View(context);
        dot.setBackground(rounded(context, connected ? colors.accent() : withAlpha(colors.text(), 0.55f), 4));
        var dotParams = new LinearLayout.LayoutParams(dp(context, 8), dp(context, 8));
        dotParams.setMarginEnd(dp(context, 8));
        row.addView(dot, dotParams);
        var labels = column(context);
        item(labels, label(context, context.getString(R.string.health_connect_status), colors.text(), 13, 600, false));
        if (!showBadge) {
            item(labels, label(context, message, colors.secondary(), 13), 6);
        }
        weighted(row, labels);
        if (showBadge) {
            var badge = label(context, message, connected ? colors.accent() : colors.text(), 13, 600, false);
            badge.setBackground(rounded(context, connected ? colors.track() : colors.divider(), 24));
            pad(badge, 10, 4);
            row.addView(badge);
        }
        item(healthStatus, row);
        if (status == HealthConnectSyncState.PermissionRequired) {
            var button = action(context, context.getString(R.string.allow_steps_access), colors, onConnect);
            button.setTextColor(colors.background());
            button.setBackground(ripple(context, colors.background(), 12, colors.accent()));
            button.setGravity(Gravity.CENTER);
            pad(button, 16, 4);
            item(healthStatus, button, 12);
        } else if (status == HealthConnectSyncState.Error) {
            var button = action(context, context.getString(R.string.retry), colors, onRetry);
            pad(button, 0, 4);
            item(healthStatus, button, 12);
        }
    }

    private RadioButton themeOption(AppTheme theme, Consumer<AppTheme> onSetTheme) {
        var button = new RadioButton(context);
        button.setId(View.generateViewId());
        button.setButtonDrawable((android.graphics.drawable.Drawable) null);
        button.setBackground(ripple(context, colors.accent(), 8, null));
        button.setMinHeight(dp(context, 56));
        pad(button, 0, 8);
        button.setText(theme.label());
        button.setTextColor(colors.text());
        button.setTextSize(15);
        button.setIncludeFontPadding(false);
        button.setOnClickListener(view -> onSetTheme.accept(theme));
        return button;
    }

    private View backupRow(int title, int subtitle, Runnable onClick) {
        var row = row(context);
        String description = context.getString(title);
        String summary = context.getString(subtitle);
        row.setContentDescription(description + ", " + summary);
        row.setScreenReaderFocusable(true);
        row.setMinimumHeight(dp(context, 60));
        row.setBackground(ripple(context, colors.accent(), 8, null));
        pad(row, 0, 10);
        var labels = column(context);
        labels.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        item(labels, label(context, description, colors.accent(), 15, 600, false));
        item(labels, label(context, summary, colors.secondary(), 13), 3);
        weighted(row, labels);
        var arrow = label(context, "›", colors.secondary(), 22);
        arrow.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        row.addView(arrow);
        row.setOnClickListener(view -> onClick.run());
        return row;
    }

    private Button link(int title, String url, String arrow) {
        var button = action(context, context.getString(title) + "  " + arrow, colors, () -> {
            try {
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
            } catch (ActivityNotFoundException ignored) {
                // A browser is optional on the device.
            }
        });
        button.setTextSize(14);
        pad(button, 0, 8);
        return button;
    }

    private Button goalButton(int amount, IntConsumer onAdjust) {
        boolean increasing = amount > 0;
        var button = action(context, increasing ? "+" : "−", colors, () -> onAdjust.accept(amount));
        button.setContentDescription(context.getString(
                increasing ? R.string.increase_goal : R.string.decrease_goal, Math.abs(amount)));
        button.setTextSize(24);
        button.setGravity(Gravity.CENTER);
        button.setBackground(ripple(context, colors.accent(), 24, colors.surface()));
        class Repeat implements Runnable {
            private long delay;

            @Override public void run() {
                if (button.isAttachedToWindow() && button.isEnabled() && button.isPressed()) {
                    onAdjust.accept(amount);
                    button.postDelayed(this, delay);
                    delay = nextGoalRepeatDelay(delay);
                }
            }

            void start() {
                delay = nextGoalRepeatDelay(FIRST_GOAL_REPEAT_DELAY);
                button.postDelayed(this, FIRST_GOAL_REPEAT_DELAY);
            }
        }
        var repeat = new Repeat();
        button.setOnTouchListener((view, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                button.removeCallbacks(repeat);
            }
            return false;
        });
        button.setOnLongClickListener(view -> {
            onAdjust.accept(amount);
            repeat.start();
            return true;
        });
        return button;
    }

    static long nextGoalRepeatDelay(long delayMillis) {
        return Math.max(45, delayMillis - 15);
    }

    private static void liveRegion(View view) {
        view.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
    }

    static int statusMessage(HealthConnectSyncState status) {
        return switch (status) {
            case Loading, Syncing -> R.string.health_connect_syncing;
            case PermissionRequired -> R.string.health_connect_disconnected;
            case Unavailable -> R.string.health_connect_unavailable;
            case UpdateRequired -> R.string.health_connect_update_required;
            case NoStepCounter -> R.string.health_connect_no_step_counter;
            case Error -> R.string.health_connect_error;
            case Ready -> R.string.health_connect_connected;
        };
    }
}
