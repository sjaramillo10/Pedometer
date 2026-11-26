package dev.sjaramillo.nemi.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.icu.number.Notation
import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.text.ConstrainedFieldPosition
import android.icu.text.DateFormat
import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.icu.util.TimeZone
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView
import dev.sjaramillo.nemi.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.format.TextStyle as DayTextStyle

internal fun Context.todayChart(
    state: HomeUiState,
    colors: Palette,
): View {
    val today = state.today
    val numbers = NumberFormat.getIntegerInstance(resources.configuration.locales[0])
    val summary =
        column().apply {
            item(label(today?.let { numbers.format(it.steps) } ?: "—", colors.text, 38, 700, centered = true).apply { autoSize(10, 38) })
            if (today == null) {
                item(label(getString(R.string.no_steps_today), colors.secondary, 13, centered = true))
            } else {
                item(label(getString(R.string.steps_unit), colors.secondary, 14, centered = true))
                if (today.fromBackup) item(label(getString(R.string.from_backup), colors.secondary, 12, centered = true))
                if (state.reachedGoal(today.steps)) item(label(getString(R.string.goal_reached), colors.accent, 12, centered = true))
            }
        }
    val chart =
        object : FrameLayout(this) {
            override fun onMeasure(
                widthMeasureSpec: Int,
                heightMeasureSpec: Int,
            ) {
                val diameter = minOf(MeasureSpec.getSize(widthMeasureSpec), dp(214))
                summary.layoutParams.width = (diameter * 0.72f).toInt()
                val square = MeasureSpec.makeMeasureSpec(diameter, MeasureSpec.EXACTLY)
                super.onMeasure(square, square)
            }
        }.apply {
            addView(GoalRing(context, colors, state.progress), FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            addView(summary, FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER))
        }

    return card(R.string.today, colors).apply {
        addView(
            chart,
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                gravity = Gravity.CENTER_HORIZONTAL
                topMargin = dp(12)
                bottomMargin = dp(20)
            },
        )
    }
}

private class GoalRing(
    context: Context,
    private val colors: Palette,
    private val progress: Float,
) : View(context) {
    private val paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = context.dp(20).toFloat()
            strokeCap = Paint.Cap.ROUND
        }

    init {
        contentDescription = context.getString(R.string.daily_goal_progress)
    }

    override fun onDraw(canvas: Canvas) {
        val inset = paint.strokeWidth / 2
        paint.color = colors.track
        canvas.drawCircle(width / 2f, height / 2f, width / 2f - inset, paint)
        if (progress > 0f) {
            paint.color = colors.accent
            canvas.drawArc(inset, inset, width - inset, height - inset, -90f, 360f * progress, false, paint)
        }
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.ProgressBar"
        info.rangeInfo = AccessibilityNodeInfo.RangeInfo(AccessibilityNodeInfo.RangeInfo.RANGE_TYPE_PERCENT, 0f, 1f, progress)
    }
}

internal fun Context.weekChart(
    state: HomeUiState,
    colors: Palette,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
): View {
    val locale = resources.configuration.locales[0]
    val week = state.week
    val maximum = week.scaleMaximum(state.goal)
    val numbers = NumberFormat.getIntegerInstance(locale)
    val unit = getString(R.string.steps_unit)
    return card(R.string.weekly_rhythm, colors).apply {
        item(
            row().apply {
                addView(weekNavigation(R.string.previous_week, false, colors, enabled = true, onPreviousWeek))
                weighted(
                    label(
                        displayWeekRange(week.days.first().date, week.days.last().date, state.date.year, locale),
                        colors.secondary,
                        14,
                        centered = true,
                    ).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE },
                )
                addView(weekNavigation(R.string.next_week, true, colors, week.range.canGoNext, onNextWeek))
            },
        )
        if (week.isEmpty) item(label(getString(R.string.no_steps_for_week), colors.text).apply { pad(16, 0) }, top = 12)
        item(
            HorizontalScrollView(context).apply {
                isFillViewport = true
                isHorizontalScrollBarEnabled = false
                addView(
                    row().apply {
                        minimumWidth = dp(294)
                        week.days.forEach { day -> weighted(weekBar(day, state.date, locale, maximum, state.goal, colors)) }
                    },
                    FrameLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT),
                )
            },
            top = 12,
        )
        item(divider(colors), top = 12, height = dp(1))
        item(
            metricPair(
                colors,
                stepMetric(R.string.week_total, numbers.format(week.total), unit, colors),
                stepMetric(R.string.week_average, numbers.format(week.average), unit, colors),
            ),
            top = 12,
        )
    }
}

private fun Context.weekNavigation(
    label: Int,
    forward: Boolean,
    colors: Palette,
    enabled: Boolean,
    onClick: () -> Unit,
): View =
    action("", colors, onClick).apply {
        contentDescription = getString(label)
        isEnabled = enabled
        val chevron =
            object : android.graphics.drawable.Drawable() {
                private val paint =
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (enabled) colors.accent else colors.text.withAlpha(0.38f)
                        style = Paint.Style.STROKE
                        strokeWidth = context.dp(2).toFloat()
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                    }
                private val path = Path()

                override fun draw(canvas: Canvas) {
                    val x = bounds.exactCenterX()
                    val y = bounds.exactCenterY()
                    val pointsRight = forward == (this@apply.layoutDirection != View.LAYOUT_DIRECTION_RTL)
                    val offset = context.dp(2).toFloat() * if (pointsRight) 1f else -1f
                    path.rewind()
                    path.moveTo(x - offset, y - context.dp(5))
                    path.lineTo(x + offset, y)
                    path.lineTo(x - offset, y + context.dp(5))
                    canvas.drawPath(path, paint)
                }

                override fun setAlpha(alpha: Int) {
                    paint.alpha = alpha
                }

                override fun setColorFilter(colorFilter: android.graphics.ColorFilter?) {
                    paint.colorFilter = colorFilter
                }

                @Deprecated("Required by Drawable")
                override fun getOpacity(): Int = android.graphics.PixelFormat.TRANSLUCENT
            }
        foreground = chevron
        foregroundGravity = Gravity.FILL
    }

private fun Context.weekBar(
    day: WeekDay,
    today: LocalDate,
    locale: Locale,
    maximum: Long,
    goal: Int,
    colors: Palette,
): View {
    val steps = day.total?.steps
    val reached = steps != null && steps >= goal
    val date = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(day.date)
    val total =
        when {
            steps != null -> getString(R.string.step_count, NumberFormat.getIntegerInstance(locale).format(steps))
            day.date > today -> getString(R.string.future_day)
            else -> getString(R.string.no_steps_for_day)
        }
    return column().apply {
        isScreenReaderFocusable = true
        contentDescription =
            listOfNotNull(
                date,
                total,
                getString(R.string.goal_reached).takeIf { reached },
                getString(R.string.from_backup).takeIf { day.total?.fromBackup == true },
            ).joinToString(", ")
        pad(2, 4)
        item(WeekPlot(context, colors, steps, maximum, reached), height = dp(104))
        val compact =
            steps?.let {
                NumberFormatter
                    .withLocale(
                        locale,
                    ).notation(Notation.compactShort())
                    .precision(Precision.maxFraction(1))
                    .format(it)
                    .toString()
            } ?: "—"
        val weekday = day.date.dayOfWeek.getDisplayName(DayTextStyle.SHORT_STANDALONE, locale)
        item(chartLabel(compact, colors.text), top = 4)
        item(chartLabel(weekday, if (day.date == today) colors.accent else colors.text, if (day.date == today) 600 else 400), top = 4)
    }
}

private fun Context.chartLabel(
    value: String,
    color: Int,
    weight: Int = 400,
): TextView =
    label(value, color, 13, weight, centered = true).apply {
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        autoSize(9, 13)
    }

private fun TextView.autoSize(
    min: Int,
    max: Int,
) {
    maxLines = 1
    setAutoSizeTextTypeUniformWithConfiguration(min, max, 1, TypedValue.COMPLEX_UNIT_SP)
}

private class WeekPlot(
    context: Context,
    private val colors: Palette,
    private val steps: Long?,
    private val maximum: Long,
    private val reached: Boolean,
) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val check = Path()

    override fun onDraw(canvas: Canvas) {
        val barWidth = width * 0.55f
        val left = (width - barWidth) / 2
        val right = left + barWidth
        val bottom = height.toFloat()
        paint.color = if (reached) colors.accent else colors.bar
        paint.style = Paint.Style.FILL
        if (steps == null) {
            canvas.drawRect(left, bottom - context.dp(1), right, bottom, paint)
            return
        }

        val barHeight = ((height - context.dp(16)) * (steps.toDouble() / maximum)).toFloat().coerceAtLeast(context.dp(2).toFloat())
        val top = bottom - barHeight
        val radius = minOf(context.dp(5).toFloat(), barHeight / 2)
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
        if (reached) {
            val center = width / 2f
            val y = top - context.dp(8)
            check.rewind()
            check.moveTo(center - context.dp(4), y)
            check.lineTo(center - context.dp(1), y + context.dp(3))
            check.lineTo(center + context.dp(5), y - context.dp(3))
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = context.dp(2).toFloat()
            paint.strokeCap = Paint.Cap.ROUND
            paint.strokeJoin = Paint.Join.ROUND
            canvas.drawPath(check, paint)
        }
    }
}

private fun displayWeekRange(
    start: LocalDate,
    end: LocalDate,
    currentYear: Int,
    locale: Locale,
): String {
    val showYear = start.year != currentYear || end.year != currentYear
    val skeleton = if (showYear) "yyMMMd" else "MMMd"
    val format = DateIntervalFormat.getInstance(skeleton, locale)
    format.timeZone = TimeZone.getTimeZone("UTC")
    val interval = DateInterval(start.toEpochDay() * 86_400_000L, end.toEpochDay() * 86_400_000L)
    val formatted = format.formatToValue(interval)
    if (!showYear || locale.language != "en") return formatted.toString()

    val result = StringBuilder(formatted.toString())
    val year = ConstrainedFieldPosition().apply { constrainField(DateFormat.Field.YEAR) }
    var added = 0
    while (formatted.nextPosition(year)) {
        val shortYear = formatted.subSequence(year.start, year.limit).takeLast(2)
        result.replace(year.start + added, year.limit + added, "’$shortYear")
        added += 1 + shortYear.length - (year.limit - year.start)
    }
    return result.toString()
}
