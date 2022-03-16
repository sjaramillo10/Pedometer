package dev.sjaramillo.pedometer.ui.stats

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.sjaramillo.pedometer.R

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel()) {

    val uiState = viewModel.uiState.collectAsState()

    val uiStateValue = uiState.value

    if (uiStateValue is StatsUiState.Success) {
        val statsData = uiStateValue.statsData

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.spacing_medium))
        ) {
            ListItem(
                value = stringResource(
                    id = R.string.stats_record_format,
                    statsData.recordSteps,
                    statsData.recordDate
                ),
                description = stringResource(id = R.string.stats_record),
            )
            Row {
                ListItem(
                    value = statsData.totalStepsLast7Days,
                    description = stringResource(id = R.string.stats_total_last_7_days),
                    modifier = Modifier.weight(1f),
                )
                ListItem(
                    value = statsData.averageStepsLast7Days,
                    description = stringResource(id = R.string.stats_average_last_7_days),
                    modifier = Modifier.weight(1f),
                )
            }
            Row {
                ListItem(
                    value = statsData.totalStepsThisMonth,
                    description = stringResource(id = R.string.stats_total_this_month),
                    modifier = Modifier.weight(1f),
                )
                ListItem(
                    value = statsData.averageStepsThisMonth,
                    description = stringResource(id = R.string.stats_average_this_month),
                    modifier = Modifier.weight(1f),
                )
            }
            Row {
                ListItem(
                    value = statsData.totalStepsThisYear,
                    description = stringResource(id = R.string.stats_total_this_year),
                    modifier = Modifier.weight(1f),
                )
                ListItem(
                    value = statsData.averageStepsThisYear,
                    description = stringResource(id = R.string.stats_average_this_year),
                    modifier = Modifier.weight(1f),
                )
            }
            Row {
                ListItem(
                    value = statsData.totalStepsAllTime,
                    description = stringResource(id = R.string.stats_total_all_time),
                    modifier = Modifier.weight(1f),
                )
                ListItem(
                    value = statsData.averageStepsAllTime,
                    description = stringResource(id = R.string.stats_average_all_time),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }
    }
}

@Composable
fun ListItem(value: String, description: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MaterialTheme.typography.h6,
            modifier = Modifier
                .fillMaxWidth()
        )
        Text(
            text = description,
            style = MaterialTheme.typography.body2,
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
