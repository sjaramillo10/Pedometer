package dev.sjaramillo.pedometer.ui

import android.icu.number.Notation
import android.icu.number.NumberFormatter
import android.icu.number.Precision
import android.icu.text.ConstrainedFieldPosition
import android.icu.text.DateFormat
import android.icu.text.DateIntervalFormat
import android.icu.util.DateInterval
import android.icu.util.TimeZone
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.sjaramillo.pedometer.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.time.format.TextStyle as DayTextStyle

@Composable
internal fun TodayChart(state: HomeUiState) {
    val palette = LocalPalette.current
    val today = state.today
    val locale = LocalConfiguration.current.locales[0]
    val progressLabel = stringResource(R.string.daily_goal_progress)
    SectionCard(stringResource(R.string.today)) {
        val progress = state.progress
        Box(
            modifier =
                Modifier
                    .padding(top = 12.dp, bottom = 20.dp)
                    .fillMaxWidth()
                    .wrapContentWidth()
                    .widthIn(max = 214.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                Modifier.matchParentSize().semantics {
                    contentDescription = progressLabel
                    progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)
                },
            ) {
                val stroke = 20.dp.toPx()
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
                    autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = 38.sp),
                )
                if (today == null) {
                    AppText(stringResource(R.string.no_steps_today), size = 13, secondary = true, align = TextAlign.Center)
                } else {
                    AppText(stringResource(R.string.steps_unit), size = 14, secondary = true, align = TextAlign.Center)
                    if (today.fromBackup) {
                        AppText(stringResource(R.string.from_backup), size = 12, secondary = true, align = TextAlign.Center)
                    }
                    if (state.reachedGoal(today.steps)) {
                        AppText(stringResource(R.string.goal_reached), size = 12, accent = true, align = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
internal fun WeekChart(
    state: HomeUiState,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val today = state.date
    val week = state.week
    val numberFormat = NumberFormat.getIntegerInstance(locale)
    val maximum = week.scaleMaximum(state.goal)
    val unit = stringResource(R.string.steps_unit)

    SectionCard(stringResource(R.string.weekly_rhythm)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WeekNavigationButton(stringResource(R.string.previous_week), forward = false, onClick = onPreviousWeek)
                AppText(
                    displayWeekRange(week.days.first().date, week.days.last().date, today.year, locale),
                    Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                    size = 14,
                    secondary = true,
                    align = TextAlign.Center,
                )
                WeekNavigationButton(
                    stringResource(R.string.next_week),
                    forward = true,
                    enabled = week.range.canGoNext,
                    onClick = onNextWeek,
                )
            }
            if (week.isEmpty) {
                AppText(stringResource(R.string.no_steps_for_week), Modifier.padding(horizontal = 16.dp))
            }
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()).width(maxOf(maxWidth, 294.dp)),
                ) {
                    week.days.forEach { day ->
                        WeekBar(
                            day,
                            today,
                            locale,
                            maximum,
                            state.goal,
                            Modifier.weight(1f),
                        )
                    }
                }
            }
            SectionDivider()
            MetricPair(
                Modifier.fillMaxWidth(),
                first = {
                    StepMetric(stringResource(R.string.week_total), numberFormat.format(week.total), unit)
                },
                second = {
                    StepMetric(stringResource(R.string.week_average), numberFormat.format(week.average), unit)
                },
            )
        }
    }
}

@Composable
private fun WeekNavigationButton(
    label: String,
    forward: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .size(48.dp)
            .semantics { contentDescription = label }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(16.dp)) {
            val pointsRight = forward == (layoutDirection == LayoutDirection.Ltr)
            val tip = if (pointsRight) 0.65f else 0.35f
            val tail = 1f - tip
            val chevron =
                Path().apply {
                    moveTo(size.width * tail, size.height * 0.2f)
                    lineTo(size.width * tip, size.height * 0.5f)
                    lineTo(size.width * tail, size.height * 0.8f)
                }
            drawPath(
                chevron,
                if (enabled) palette.accent else palette.text.copy(alpha = 0.38f),
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
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

@Composable
private fun WeekBar(
    day: WeekDay,
    today: LocalDate,
    locale: Locale,
    maximum: Long,
    goal: Int,
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
    Column(
        modifier =
            modifier
                .semantics { contentDescription = label }
                .padding(horizontal = 2.dp)
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
            text =
                steps?.let {
                    NumberFormatter
                        .withLocale(
                            locale,
                        ).notation(Notation.compactShort())
                        .precision(Precision.maxFraction(1))
                        .format(it)
                        .toString()
                } ?: "—",
            modifier = Modifier.clearAndSetSemantics {},
            style = TextStyle(color = palette.text, textAlign = TextAlign.Center),
            maxLines = 1,
            autoSize = TextAutoSize.StepBased(minFontSize = 9.sp, maxFontSize = 13.sp),
        )
        BasicText(
            text = day.date.dayOfWeek.getDisplayName(DayTextStyle.SHORT_STANDALONE, locale),
            modifier = Modifier.clearAndSetSemantics {},
            style =
                TextStyle(
                    color = if (day.date == today) palette.accent else palette.text,
                    fontWeight = if (day.date == today) FontWeight.SemiBold else FontWeight.Normal,
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
