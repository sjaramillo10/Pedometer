package dev.sjaramillo.nemi.ui

import android.content.Context
import android.view.View
import dev.sjaramillo.nemi.R
import dev.sjaramillo.nemi.data.HealthConnectSyncState

internal class HomeScreen(
    private val context: Context,
    private val colors: Palette,
    onOpenSettings: (Boolean) -> Unit,
    private val onPreviousWeek: () -> Unit,
    private val onNextWeek: () -> Unit,
) {
    private val screen =
        ScreenLayout(context, colors, R.string.app_name, R.drawable.ic_settings, R.string.settings_title) {
            onOpenSettings(false)
        }
    private val today = context.column()
    private val week = context.column()
    private val stats = context.column()
    private val permission =
        context.column().apply {
            item(
                context.label(context.getString(R.string.health_connect_permission_required), colors.text).apply {
                    accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
                },
            )
            item(context.action(context.getString(R.string.open_settings), colors) { onOpenSettings(true) }, top = 16)
        }
    private var previous: HomeUiState? = null

    val root get() = screen.root
    val scroll get() = screen.scroll

    init {
        screen.content.item(today)
        screen.content.item(permission, top = 16)
        screen.content.item(week, top = 16)
        screen.content.item(stats, top = 16)
    }

    fun restoreScroll(position: Int) = screen.restoreScroll(position)

    fun bind(state: HomeUiState) {
        val old = previous
        if (old == state) return
        previous = state
        if (old == null || state.today != old.today || state.goal != old.goal) {
            today.removeAllViews()
            today.item(context.todayChart(state, colors))
        }
        permission.visibility = if (state.sync == HealthConnectSyncState.PermissionRequired) View.VISIBLE else View.GONE
        if (old == null || state.week != old.week || state.goal != old.goal || state.date != old.date) {
            week.removeAllViews()
            week.item(context.weekChart(state, colors, onPreviousWeek, onNextWeek))
        }
        if (old == null || state.bestDay != old.bestDay || state.lifetimeSteps != old.lifetimeSteps) {
            stats.removeAllViews()
            stats.item(context.statsSummary(state.bestDay, state.lifetimeSteps, colors))
        }
    }
}
