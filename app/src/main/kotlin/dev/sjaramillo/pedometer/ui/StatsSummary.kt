package dev.sjaramillo.pedometer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.DailySteps
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun StatsSummary(
    record: DailySteps?,
    lifetimeSteps: Long?,
) {
    val locale = LocalConfiguration.current.locales[0]
    val numbers = NumberFormat.getIntegerInstance(locale)
    val recordDate =
        record?.let {
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(LocalDate.ofEpochDay(it.day))
        }
    val empty = stringResource(R.string.no_recorded_steps)
    SectionCard(stringResource(R.string.your_journey)) {
        MetricPair(
            Modifier.fillMaxWidth(),
            first = {
                StepMetric(stringResource(R.string.best_day), record?.let { numbers.format(it.steps) } ?: "—", recordDate ?: empty)
            },
            second = {
                StepMetric(
                    stringResource(R.string.lifetime_steps),
                    lifetimeSteps?.let(numbers::format) ?: "—",
                    stringResource(R.string.steps_unit),
                )
            },
            rowDivider = { Box(Modifier.width(1.dp).height(56.dp).background(LocalPalette.current.text.copy(alpha = 0.10f))) },
        )
    }
}

@Composable
internal fun StepMetric(
    label: String,
    value: String,
    detail: String,
) {
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppText(label, size = 14, secondary = true, align = TextAlign.Center)
        AppText(value, size = 22, bold = true, align = TextAlign.Center)
        AppText(detail, size = 13, secondary = true, align = TextAlign.Center)
    }
}

@Composable
internal fun MetricPair(
    modifier: Modifier,
    first: @Composable () -> Unit,
    second: @Composable () -> Unit,
    rowDivider: (@Composable () -> Unit)? = null,
) {
    BoxWithConstraints(modifier) {
        if (maxWidth < 280.dp || LocalDensity.current.fontScale >= 1.5f) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                first()
                second()
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) { first() }
                rowDivider?.invoke()
                Box(Modifier.weight(1f)) { second() }
            }
        }
    }
}
