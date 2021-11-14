package dev.sjaramillo.pedometer.ui.stats

import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.util.DateUtil
import dev.sjaramillo.pedometer.util.FormatUtil
import java.time.format.DateTimeFormatter

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {

    val statsData = viewModel.getStatsData().collectAsState(initial = StatsData())

    // TODO Should move all this formatting to ViewModel?
    val record = statsData.value.record
    val recordSteps = FormatUtil.numberFormat.format(record.steps)
    val recordDate = DateTimeFormatter.ofPattern("d MMM uuuu")
        .format(DateUtil.dayToLocalDate(record.day))
    val totalLast7Days = FormatUtil.numberFormat.format(statsData.value.totalLast7Days)
    val averageLast7Days = FormatUtil.numberFormat.format(statsData.value.averageLast7Days)
    val totalThisMonth = FormatUtil.numberFormat.format(statsData.value.totalThisMonth)
    val averageThisMonth = FormatUtil.numberFormat.format(statsData.value.averageThisMonth)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.spacing_medium))
    ) {
        ListItem(
            value = stringResource(id = R.string.stats_record_format, recordSteps, recordDate),
            description = stringResource(id = R.string.stats_record),
        )
        ListItem(
            value = totalLast7Days,
            description = stringResource(id = R.string.stats_total_last_7_days),
        )
        ListItem(
            value = averageLast7Days,
            description = stringResource(id = R.string.stats_average_last_7_days),
        )
        ListItem(
            value = totalThisMonth,
            description = stringResource(id = R.string.stats_total_this_month),
        )
        ListItem(
            value = averageThisMonth,
            description = stringResource(id = R.string.stats_average_this_month),
        )
    }
}

@Composable
fun ListItem(value: String, description: String) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .fillMaxWidth()
        )
        Text(
            text = description,
            style = MaterialTheme.typography.body1,
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.spacing_medium)))
    }
}

@Preview
@Composable
fun ListItemPreview() {
    ListItem(value = "10,000", description = "Record")
}
