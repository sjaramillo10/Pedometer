package dev.sjaramillo.nemi.ui;

import static dev.sjaramillo.nemi.ui.AppComponents.*;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup.MarginLayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import dev.sjaramillo.nemi.R;
import dev.sjaramillo.nemi.data.HealthConnectSyncState;
import java.util.Objects;
import java.util.function.Consumer;

final class HomeScreen {
    private final Context context;
    private final Palette colors;
    private final Runnable onPreviousWeek;
    private final Runnable onNextWeek;
    private final ScreenLayout screen;
    private final LinearLayout today;
    private final LinearLayout week;
    private final LinearLayout stats;
    private final LinearLayout healthStatus;
    private final TextView healthMessage;
    private final Button healthAction;
    private HomeUiState previous;

    HomeScreen(Context context, Palette colors, Consumer<Boolean> onOpenSettings,
            Runnable onPreviousWeek, Runnable onNextWeek) {
        this.context = context;
        this.colors = colors;
        this.onPreviousWeek = onPreviousWeek;
        this.onNextWeek = onNextWeek;
        screen = new ScreenLayout(context, colors, R.string.app_name, R.drawable.ic_settings,
                R.string.settings_title, false, () -> onOpenSettings.accept(false));
        today = column(context);
        week = column(context);
        stats = column(context);
        healthMessage = label(context, "", colors.secondary(), 14);
        healthMessage.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        healthAction = action(context, context.getString(R.string.view_health_connect_status),
                colors, () -> onOpenSettings.accept(true));
        pad(healthAction, 0, 4);
        healthStatus = card(context, R.string.health_connect, colors);
        item(healthStatus, healthMessage);
        item(healthStatus, healthAction, 8);
        item(screen.content, healthStatus);
        item(screen.content, today, 16);
        item(screen.content, week, 16);
        item(screen.content, stats, 16);
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

    void bind(HomeUiState state) {
        HomeUiState old = previous;
        if (state.equals(old)) {
            return;
        }
        previous = state;
        if (old == null || !Objects.equals(state.today(), old.today()) || state.goal() != old.goal()
                || !Objects.equals(state.todayEmptyMessage(), old.todayEmptyMessage())) {
            today.removeAllViews();
            item(today, StepCharts.todayChart(context, state, colors));
        }
        int margin = state.sync() == HealthConnectSyncState.Ready ? 0 : dp(context, 16);
        var layout = (MarginLayoutParams) today.getLayoutParams();
        if (layout.topMargin != margin) {
            layout.topMargin = margin;
            today.setLayoutParams(layout);
        }
        if (old == null || old.sync() != state.sync()) {
            boolean refreshing = state.sync() == HealthConnectSyncState.Loading
                    || state.sync() == HealthConnectSyncState.Syncing;
            healthStatus.setVisibility(state.sync() == HealthConnectSyncState.Ready ? View.GONE : View.VISIBLE);
            healthMessage.setText(refreshing ? R.string.health_connect_syncing : R.string.check_health_connect_status);
            healthAction.setVisibility(refreshing ? View.GONE : View.VISIBLE);
        }
        if (old == null || !state.week().equals(old.week()) || state.goal() != old.goal()
                || !state.date().equals(old.date())) {
            week.removeAllViews();
            item(week, StepCharts.weekChart(context, state, colors, onPreviousWeek, onNextWeek));
        }
        if (old == null || !Objects.equals(state.bestDay(), old.bestDay())
                || !Objects.equals(state.lifetimeSteps(), old.lifetimeSteps())) {
            stats.removeAllViews();
            item(stats, StatsSummary.create(context, state.bestDay(), state.lifetimeSteps(), colors));
        }
    }
}
