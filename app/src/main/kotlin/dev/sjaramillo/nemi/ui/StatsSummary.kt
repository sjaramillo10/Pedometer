package dev.sjaramillo.nemi.ui

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import dev.sjaramillo.nemi.R
import dev.sjaramillo.nemi.data.DailySteps
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun Context.statsSummary(
    record: DailySteps?,
    lifetimeSteps: Long?,
    colors: Palette,
): View {
    val locale = resources.configuration.locales[0]
    val numbers = NumberFormat.getIntegerInstance(locale)
    val recordDate =
        record?.let {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(LocalDate.ofEpochDay(it.day))
        }
    return card(R.string.your_journey, colors).apply {
        item(
            metricPair(
                colors,
                stepMetric(
                    R.string.best_day,
                    record?.let { numbers.format(it.steps) } ?: "—",
                    recordDate ?: getString(R.string.no_recorded_steps),
                    colors,
                ),
                stepMetric(R.string.lifetime_steps, lifetimeSteps?.let(numbers::format) ?: "—", getString(R.string.steps_unit), colors),
            ),
        )
        item(divider(colors), top = 16, height = dp(1))
        item(label(getString(R.string.journey_slogan), colors.secondary, 13, centered = true), top = 12)
    }
}

internal fun Context.stepMetric(
    title: Int,
    value: String,
    detail: String,
    colors: Palette,
): View =
    column().apply {
        isScreenReaderFocusable = true
        item(label(getString(title), colors.secondary, 14, centered = true))
        item(label(value, colors.text, 22, 600, centered = true), top = 4)
        item(label(detail, colors.secondary, 13, centered = true), top = 4)
    }

internal fun Context.metricPair(
    colors: Palette,
    first: View,
    second: View,
): View =
    if (resources.configuration.screenWidthDp < 352 || resources.configuration.fontScale >= 1.5f) {
        column().apply {
            item(first)
            item(second, top = 12)
        }
    } else {
        row().apply {
            gravity = Gravity.CENTER_VERTICAL
            weighted(first)
            addView(
                divider(colors),
                LinearLayout.LayoutParams(dp(1), dp(56)).apply {
                    marginStart = dp(12)
                    marginEnd = dp(12)
                },
            )
            weighted(second)
        }
    }
