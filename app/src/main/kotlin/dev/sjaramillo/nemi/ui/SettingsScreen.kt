package dev.sjaramillo.nemi.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import dev.sjaramillo.nemi.R
import dev.sjaramillo.nemi.data.HealthConnectSyncState
import kotlin.math.abs

internal const val FIRST_GOAL_REPEAT_DELAY = 150L

internal fun nextGoalRepeatDelay(delayMillis: Long): Long = (delayMillis - 15L).coerceAtLeast(45L)

internal class SettingsScreen(
    private val context: Context,
    private val colors: Palette,
    onBack: () -> Unit,
    private val onConnect: () -> Unit,
    private val onRetry: () -> Unit,
    onAdjustGoal: (Int) -> Unit,
    onSetTheme: (AppTheme) -> Unit,
    onImport: () -> Unit,
    onExport: () -> Unit,
    onDismissFileMessage: () -> Unit,
) {
    private val screen =
        ScreenLayout(context, colors, R.string.settings_title, R.drawable.ic_back, R.string.back, actionAtStart = true, onAction = onBack)
    private val goal = context.label("", colors.text, 24, 600, centered = true)
    private val decrease = goalButton(-100, onAdjustGoal)
    private val increase = goalButton(100, onAdjustGoal)
    private val themeOptions = AppTheme.entries.associateWith { theme -> themeOption(theme, onSetTheme) }
    private val healthStatus = context.column()
    private val import = backupRow(R.string.import_title, R.string.import_summary, onImport)
    private val export = backupRow(R.string.export_title, R.string.export_summary, onExport)
    private val working = context.label(context.getString(R.string.backup_working), colors.text).apply { liveRegion() }
    private val result = context.label("", colors.text).apply { liveRegion() }
    private val dismiss = context.action(context.getString(R.string.dismiss_result), colors, onDismissFileMessage)
    private val version = context.label("", colors.secondary, 13, centered = true)
    private val healthCard =
        context.card(R.string.health_connect, colors).apply {
            item(context.label(context.getString(R.string.health_connect_description), colors.secondary, 14))
            item(healthStatus, top = 12)
            item(link(R.string.learn_health_connect, "https://developer.android.com/health-and-fitness/health-connect/get-started", "›"))
        }
    private var previous: SettingsUiState? = null

    val root get() = screen.root
    val scroll get() = screen.scroll

    init {
        screen.content.apply {
            item(
                context.card(R.string.daily_step_goal, colors).apply {
                    item(
                        context.row().apply {
                            background = context.rounded(colors.track, 16)
                            pad(8)
                            addView(decrease)
                            weighted(
                                context.column().apply {
                                    liveRegion()
                                    pad(8, 0)
                                    item(goal)
                                    item(context.label(context.getString(R.string.steps_per_day), colors.secondary, 14, centered = true))
                                },
                            )
                            addView(increase)
                        },
                    )
                },
            )
            item(
                context.card(R.string.appearance, colors).apply {
                    item(
                        RadioGroup(context).apply {
                            themeOptions.values.forEachIndexed { index, button ->
                                if (index > 0) addView(context.divider(colors), RadioGroup.LayoutParams(-1, context.dp(1)))
                                addView(button, RadioGroup.LayoutParams(-1, WRAP_CONTENT))
                            }
                        },
                    )
                },
                top = 16,
            )
            item(healthCard, top = 16)
            item(
                context.card(R.string.backup, colors).apply {
                    item(import)
                    item(context.divider(colors), height = context.dp(1))
                    item(export)
                    item(context.divider(colors), height = context.dp(1))
                    item(context.label(context.getString(R.string.backup_precedence), colors.secondary, 13), top = 12)
                    item(working, top = 6)
                    item(result, top = 6)
                    item(dismiss)
                },
                top = 16,
            )
            item(
                context.card(R.string.about, colors).apply {
                    item(version)
                    item(aboutText(R.string.about_text), top = 16)
                    item(aboutText(R.string.original_project), top = 10)
                    item(context.divider(colors), top = 10, height = context.dp(1))
                    item(link(R.string.source_code, "https://github.com/sjaramillo10/Nemi", "↗"))
                },
                top = 16,
            )
        }
    }

    fun bind(state: SettingsUiState) {
        val old = previous
        if (old == state) return
        previous = state
        if (old?.goal != state.goal) {
            goal.text = context.getString(R.string.selected_goal, state.goal)
            decrease.isEnabled = adjustGoal(state.goal, -100) != state.goal
            increase.isEnabled = adjustGoal(state.goal, 100) != state.goal
        }
        if (old?.theme != state.theme) {
            for ((theme, button) in themeOptions) {
                val selected = theme == state.theme
                button.isChecked = selected
                val tint = if (selected) colors.accent else colors.text.withAlpha(0.40f)
                val ring =
                    GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setStroke(context.dp(if (selected) 7 else 1), tint)
                        setBounds(0, 0, context.dp(20), context.dp(20))
                    }
                button.setCompoundDrawablesRelative(null, null, ring, null)
            }
        }
        if (old?.sync != state.sync) bindHealthStatus(state.sync)
        if (old?.version != state.version) version.text = context.getString(R.string.about_app_version, state.version)
        if (old?.file != state.file) {
            import.isEnabled = !state.file.busy
            export.isEnabled = !state.file.busy
            working.visibility = if (state.file.busy) View.VISIBLE else View.GONE
            result.text = state.file.message
            result.visibility = if (state.file.message == null) View.GONE else View.VISIBLE
            dismiss.visibility = result.visibility
        }
    }

    fun restoreScroll(position: Int) = screen.restoreScroll(position)

    fun revealHealthConnect() {
        healthCard.post {
            val bounds = Rect(0, 0, healthCard.width, healthCard.height)
            healthCard.requestRectangleOnScreen(bounds, false)
        }
    }

    private fun bindHealthStatus(status: HealthConnectSyncState) {
        healthStatus.removeAllViews()
        val message =
            when (status) {
                HealthConnectSyncState.Loading, HealthConnectSyncState.Syncing -> R.string.health_connect_syncing
                HealthConnectSyncState.PermissionRequired -> R.string.health_connect_disconnected
                HealthConnectSyncState.Unavailable -> R.string.health_connect_unavailable
                HealthConnectSyncState.Error -> R.string.health_connect_error
                HealthConnectSyncState.Ready -> R.string.health_connect_connected
            }
        val connected = status == HealthConnectSyncState.Ready
        healthStatus.item(
            context.row().apply {
                liveRegion()
                background = context.rounded(colors.background, 16)
                pad(12)
                addView(
                    View(context).apply {
                        background = context.rounded(if (connected) colors.accent else colors.text.withAlpha(0.55f), 4)
                    },
                    LinearLayout.LayoutParams(context.dp(8), context.dp(8)).apply { marginEnd = context.dp(8) },
                )
                weighted(
                    context.column().apply {
                        item(context.label(context.getString(R.string.health_connect_status), colors.text, 13, 600))
                        if (!connected) item(context.label(context.getString(message), colors.secondary, 13), top = 6)
                    },
                )
                if (connected) {
                    addView(
                        context.label(context.getString(message), colors.accent, 13, 600).apply {
                            background = context.rounded(colors.track, 24)
                            pad(10, 4)
                        },
                    )
                }
            },
        )
        when (status) {
            HealthConnectSyncState.PermissionRequired ->
                healthStatus.item(context.action(context.getString(R.string.connect_health_connect), colors, onConnect), top = 12)
            HealthConnectSyncState.Error -> healthStatus.item(context.action(context.getString(R.string.retry), colors, onRetry), top = 12)
            else -> Unit
        }
    }

    private fun themeOption(
        theme: AppTheme,
        onSetTheme: (AppTheme) -> Unit,
    ): RadioButton =
        RadioButton(context).apply {
            id = View.generateViewId()
            buttonDrawable = null
            background = context.ripple(colors.accent)
            minHeight = context.dp(56)
            pad(0, 8)
            text = context.getString(theme.label)
            setTextColor(colors.text)
            textSize = 15f
            includeFontPadding = false
            setOnClickListener { onSetTheme(theme) }
        }

    private fun backupRow(
        title: Int,
        subtitle: Int,
        onClick: () -> Unit,
    ): View =
        context.row().apply {
            val description = context.getString(title)
            val summary = context.getString(subtitle)
            contentDescription = "$description, $summary"
            isScreenReaderFocusable = true
            minimumHeight = context.dp(60)
            background = context.ripple(colors.accent)
            pad(0, 10)
            weighted(
                context.column().apply {
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                    item(context.label(description, colors.accent, 15, 600))
                    item(context.label(summary, colors.secondary, 13), top = 3)
                },
            )
            addView(context.label("›", colors.secondary, 22).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO })
            setOnClickListener { onClick() }
        }

    private fun aboutText(resource: Int): TextView =
        context.label(Html.fromHtml(context.getString(resource), Html.FROM_HTML_MODE_COMPACT), colors.text, 14).apply {
            movementMethod = LinkMovementMethod.getInstance()
            setLinkTextColor(colors.accent)
        }

    private fun link(
        label: Int,
        url: String,
        arrow: String,
    ): Button =
        context
            .action("${context.getString(label)}  $arrow", colors) {
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (_: ActivityNotFoundException) {
                    // A browser is optional on the device.
                }
            }.apply {
                textSize = 14f
                pad(0, 8)
            }

    private fun goalButton(
        amount: Int,
        onAdjust: (Int) -> Unit,
    ): Button {
        val increasing = amount > 0
        val label = context.getString(if (increasing) R.string.increase_goal else R.string.decrease_goal, abs(amount))
        return context.action(if (increasing) "+" else "−", colors) { onAdjust(amount) }.apply {
            contentDescription = label
            textSize = 24f
            gravity = Gravity.CENTER
            background = context.ripple(colors.accent, 24, colors.surface)
            val button = this
            var delay = FIRST_GOAL_REPEAT_DELAY
            val repeat =
                object : Runnable {
                    override fun run() {
                        if (button.isAttachedToWindow && button.isEnabled && button.isPressed) {
                            onAdjust(amount)
                            button.postDelayed(this, delay)
                            delay = nextGoalRepeatDelay(delay)
                        }
                    }
                }
            setOnTouchListener { _, event ->
                if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                    removeCallbacks(repeat)
                }
                false
            }
            setOnLongClickListener {
                onAdjust(amount)
                postDelayed(repeat, FIRST_GOAL_REPEAT_DELAY)
                delay = nextGoalRepeatDelay(FIRST_GOAL_REPEAT_DELAY)
                true
            }
        }
    }

    private fun View.liveRegion() {
        accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
    }
}
