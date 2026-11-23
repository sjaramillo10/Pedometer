package dev.sjaramillo.pedometer.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.ui.stats.StatsUiState
import dev.sjaramillo.pedometer.ui.stats.StatsViewModel

@Composable
internal fun StatsSummary(stats: StatsViewModel) {
    val state by stats.uiState.collectAsStateWithLifecycle()
    val data = (state as? StatsUiState.Success)?.statsData
    if (data == null) {
        AppText(stringResource(R.string.stats_loading))
        return
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Statistic(R.string.stats_record, stringResource(R.string.stats_record_format, data.recordSteps, data.recordDate))
        Statistic(R.string.stats_total_last_7_days, data.totalStepsLast7Days)
        Statistic(R.string.stats_average_last_7_days, data.averageStepsLast7Days)
        Statistic(R.string.stats_total_this_month, data.totalStepsThisMonth)
        Statistic(R.string.stats_average_this_month, data.averageStepsThisMonth)
        Statistic(R.string.stats_total_this_year, data.totalStepsThisYear)
        Statistic(R.string.stats_average_this_year, data.averageStepsThisYear)
        Statistic(R.string.stats_total_all_time, data.totalStepsAllTime)
        Statistic(R.string.stats_average_all_time, data.averageStepsAllTime)
    }
}

@Composable
private fun Statistic(
    label: Int,
    value: String,
) {
    AppText(stringResource(R.string.statistic, stringResource(label), value))
}
