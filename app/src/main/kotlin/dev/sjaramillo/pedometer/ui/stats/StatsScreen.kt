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

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {

    val statsData = viewModel.getStatsData().collectAsState(initial = StatsData())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.spacing_medium))
    ) {
        ListItem(
            value = stringResource(
                id = R.string.stats_record_format,
                statsData.value.recordSteps,
                statsData.value.recordDate
            ),
            description = stringResource(id = R.string.stats_record),
        )
        ListItem(
            value = statsData.value.totalStepsLast7Days,
            description = stringResource(id = R.string.stats_total_last_7_days),
        )
        ListItem(
            value = statsData.value.averageStepsLast7Days,
            description = stringResource(id = R.string.stats_average_last_7_days),
        )
        ListItem(
            value = statsData.value.totalStepsThisMonth,
            description = stringResource(id = R.string.stats_total_this_month),
        )
        ListItem(
            value = statsData.value.averageStepsThisMonth,
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
