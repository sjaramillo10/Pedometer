package dev.sjaramillo.pedometer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sjaramillo.pedometer.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields
import java.util.Locale
import java.time.format.TextStyle as DayTextStyle

@Composable
internal fun TodayChart(state: HomeState) {
    val palette = LocalPalette.current
    val today = state.totals.today
    val locale = LocalConfiguration.current.locales[0]
    val progressLabel = stringResource(R.string.daily_goal_progress)
    Column(
        modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(24.dp)).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(stringResource(R.string.today), Modifier.semantics { heading() }, size = 20, bold = true)
        val progress = state.progress
        Box(
            modifier = Modifier.widthIn(max = 224.dp).fillMaxWidth().aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                Modifier.matchParentSize().semantics {
                    contentDescription = progressLabel
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                },
            ) {
                val stroke = 14.dp.toPx()
                val inset = Offset(stroke / 2, stroke / 2)
                val diameter = Size(size.width - stroke, size.height - stroke)
                drawCircle(palette.track, radius = diameter.width / 2, style = Stroke(stroke))
                if (progress > 0f) {
                    drawArc(
                        color = palette.accent,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        topLeft = inset,
                        size = diameter,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth(0.72f).semantics(mergeDescendants = true) {},
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BasicText(
                    text = today?.let { NumberFormat.getIntegerInstance(locale).format(it.steps) } ?: "—",
                    style = TextStyle(color = palette.text, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
                    maxLines = 1,
                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 44.sp),
                )
                AppText(stringResource(R.string.steps_unit))
            }
        }
        AppText(stringResource(R.string.goal_summary, state.goal))
        if (today == null) AppText(stringResource(R.string.no_steps_today))
        if (today?.fromBackup == true) AppText(stringResource(R.string.from_backup))
        if (today != null && state.reachedGoal(today.steps)) {
            AppText(stringResource(R.string.goal_reached), accent = true, bold = true)
        }
    }
}

@Composable
internal fun WeekChart(state: HomeState) {
    val palette = LocalPalette.current
    val locale = LocalConfiguration.current.locales[0]
    val today = state.totals.date
    val week = state.totals.currentWeek(WeekFields.of(locale).firstDayOfWeek)
    var selectedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    val selected =
        week.firstOrNull { it.date.toEpochDay() == selectedDay && it.date <= today }
            ?: week.first { it.date == today }
    val maximum = maxOf(state.goal.toLong(), week.maxOf { it.total?.steps ?: 0L })

    Column(
        modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(24.dp)).padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppText(
            stringResource(R.string.current_week),
            Modifier.padding(horizontal = 16.dp).semantics { heading() },
            size = 20,
            bold = true,
        )
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()).width(maxOf(maxWidth, 336.dp)).selectableGroup(),
            ) {
                week.forEach { day ->
                    val isSelected = day == selected
                    val chooseDay = { selectedDay = day.date.toEpochDay() }
                    WeekBar(
                        day,
                        today,
                        locale,
                        maximum,
                        state.goal,
                        isSelected,
                        chooseDay,
                        Modifier.weight(1f),
                    )
                }
            }
        }
        Column(Modifier.padding(horizontal = 16.dp).semantics(mergeDescendants = true) {}) {
            AppText(
                if (selected.date == today) stringResource(R.string.today) else displayDate(selected.date, locale),
                bold = true,
            )
            AppText(dayTotalLabel(selected, today, locale))
            if (selected.total?.steps?.let(state::reachedGoal) == true) {
                AppText(stringResource(R.string.goal_reached), accent = true, bold = true)
            }
            if (selected.total?.fromBackup == true) AppText(stringResource(R.string.from_backup))
        }
    }
}

@Composable
private fun WeekBar(
    day: WeekDay,
    today: LocalDate,
    locale: Locale,
    maximum: Long,
    goal: Int,
    isSelected: Boolean,
    chooseDay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val steps = day.total?.steps
    val reached = steps != null && steps >= goal
    val dateLabel = displayDate(day.date, locale)
    val label =
        listOfNotNull(
            dateLabel,
            dayTotalLabel(day, today, locale),
            if (reached) stringResource(R.string.goal_reached) else null,
            if (day.total?.fromBackup == true) stringResource(R.string.from_backup) else null,
        ).joinToString(", ")
    val available = day.date <= today
    val shape = RoundedCornerShape(10.dp)
    Column(
        modifier =
            modifier
                .selectable(isSelected, enabled = available, role = Role.Tab, onClick = chooseDay)
                .semantics { contentDescription = label }
                .padding(horizontal = 2.dp)
                .background(if (isSelected) palette.track else Color.Transparent, shape)
                .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Canvas(Modifier.fillMaxWidth().height(104.dp)) {
            val barWidth = size.width * 0.55f
            val x = (size.width - barWidth) / 2
            val plotHeight = size.height - 16.dp.toPx()
            if (steps != null) {
                val barHeight = (plotHeight * (steps.toDouble() / maximum).toFloat()).coerceAtLeast(2.dp.toPx())
                val top = size.height - barHeight
                val radius = minOf(5.dp.toPx(), barHeight / 2)
                drawRoundRect(
                    if (reached) palette.accent else palette.bar,
                    topLeft = Offset(x, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(radius),
                )
                if (reached) {
                    val centerX = size.width / 2
                    val y = top - 8.dp.toPx()
                    val check =
                        Path().apply {
                            moveTo(centerX - 4.dp.toPx(), y)
                            lineTo(centerX - 1.dp.toPx(), y + 3.dp.toPx())
                            lineTo(centerX + 5.dp.toPx(), y - 3.dp.toPx())
                        }
                    drawPath(check, palette.accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                }
            } else {
                drawLine(palette.bar, Offset(x, size.height - 1.dp.toPx()), Offset(x + barWidth, size.height - 1.dp.toPx()))
            }
        }
        BasicText(
            text = day.date.dayOfWeek.getDisplayName(DayTextStyle.SHORT_STANDALONE, locale),
            modifier = Modifier.clearAndSetSemantics {},
            style =
                TextStyle(
                    color = if (isSelected) palette.accent else palette.text,
                    fontWeight = if (day.date == today || isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                ),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 13.sp),
        )
    }
}

private fun displayDate(
    date: LocalDate,
    locale: Locale,
): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(date)

@Composable
private fun dayTotalLabel(
    day: WeekDay,
    today: LocalDate,
    locale: Locale,
): String =
    when {
        day.total != null -> stringResource(R.string.step_count, NumberFormat.getIntegerInstance(locale).format(day.total.steps))
        day.date > today -> stringResource(R.string.future_day)
        else -> stringResource(R.string.no_steps_for_day)
    }
