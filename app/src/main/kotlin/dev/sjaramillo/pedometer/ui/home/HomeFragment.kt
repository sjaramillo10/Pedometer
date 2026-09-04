package dev.sjaramillo.pedometer.ui.home

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.DailySteps
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import dev.sjaramillo.pedometer.ui.MainActivity
import dev.sjaramillo.pedometer.ui.home.HomeViewModel.HomeUiState
import dev.sjaramillo.pedometer.util.DateUtil
import dev.sjaramillo.pedometer.util.FormatUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.eazegraph.lib.charts.BarChart
import org.eazegraph.lib.charts.PieChart
import org.eazegraph.lib.models.BarModel
import org.eazegraph.lib.models.PieModel
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong

@AndroidEntryPoint
class HomeFragment : Fragment() {
    private val viewModel: HomeViewModel by viewModels()

    private lateinit var stepsView: TextView
    private lateinit var unitView: TextView
    private lateinit var sliceGoal: PieModel
    private lateinit var sliceCurrent: PieModel
    private lateinit var graph: PieChart
    private lateinit var barChart: BarChart
    private lateinit var statusView: TextView
    private lateinit var connectButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        inflater.inflate(R.layout.fragment_home, container, false).also { view ->
            stepsView = view.findViewById(R.id.steps)
            unitView = view.findViewById(R.id.unit)
            graph = view.findViewById(R.id.graph)
            barChart = view.findViewById(R.id.bargraph)
            statusView = view.findViewById(R.id.health_connect_status)
            connectButton = view.findViewById(R.id.connect_health_connect)
            connectButton.setOnClickListener {
                (activity as? MainActivity)?.requestHealthConnectAccess()
            }

            sliceCurrent = PieModel("", 0f, Color.parseColor("#99CC00"))
            sliceGoal = PieModel("", 0f, Color.parseColor("#CC0000"))
            graph.addPieSlice(sliceCurrent)
            graph.addPieSlice(sliceGoal)
            graph.setOnClickListener { viewModel.toggleUnit() }
            graph.isDrawValueInPie = false
            graph.isUsePieRotation = true
            graph.startAnimation()
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.load()
        viewModel.observeHealthConnectState()
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: HomeUiState) {
        renderUnit(state)
        updatePie(state)
        updateBars(state)
        updateHealthConnectState(state.healthConnectState)
    }

    private fun renderUnit(state: HomeUiState) {
        if (!::stepsView.isInitialized) return
        unitView.text =
            if (state.showSteps) {
                getString(R.string.steps)
            } else if (state.stepSizeCm) {
                getString(R.string.distance_unit_km)
            } else {
                getString(R.string.distance_unit_mi)
            }
    }

    private fun distanceToday(
        stepsToday: Long,
        state: HomeUiState,
    ): Double {
        var distance = stepsToday * state.stepSize
        distance /= if (state.stepSizeCm) 100000f else 5280f
        return distance.toDouble()
    }

    private fun updatePie(state: HomeUiState) {
        sliceCurrent.value = state.stepsToday.toFloat()
        if (state.goal > state.stepsToday) {
            if (graph.data.size == 1) graph.addPieSlice(sliceGoal)
            sliceGoal.value = (state.goal - state.stepsToday).toFloat()
        } else {
            graph.clearChart()
            graph.addPieSlice(sliceCurrent)
        }
        graph.update()

        val numberFormat = FormatUtil.numberFormat
        stepsView.text =
            if (state.showSteps) {
                numberFormat.format(state.stepsToday)
            } else {
                numberFormat.format(distanceToday(state.stepsToday, state))
            }
    }

    private fun distanceValue(
        steps: Long,
        state: HomeUiState,
    ): Float {
        var distance = steps * state.stepSize
        distance /= if (state.stepSizeCm) 100000f else 5280f
        return (distance * 1000).roundToLong() / 1000f
    }

    private fun updateBars(state: HomeUiState) {
        if (barChart.data.isNotEmpty()) barChart.clearChart()
        barChart.isShowDecimal = !state.showSteps
        val formatter = DateTimeFormatter.ofPattern("E")

        state.lastEntries.asReversed().dropLast(1).forEach { current: DailySteps ->
            if (current.steps > 0) {
                val value =
                    if (state.showSteps) {
                        current.steps.toFloat()
                    } else {
                        distanceValue(current.steps, state)
                    }
                barChart.addBar(
                    BarModel(
                        formatter.format(DateUtil.dayToLocalDate(current.day)),
                        value,
                        if (current.steps > state.goal) Color.parseColor("#99CC00") else Color.parseColor("#0099cc"),
                    ),
                )
            }
        }
        barChart.visibility = if (barChart.data.isEmpty()) View.GONE else View.VISIBLE
        if (barChart.data.isNotEmpty()) barChart.startAnimation()
    }

    private fun updateHealthConnectState(state: HealthConnectSyncState) {
        when (state) {
            HealthConnectSyncState.PermissionRequired -> {
                statusView.setText(R.string.health_connect_permission_required)
                statusView.visibility = View.VISIBLE
                connectButton.visibility = View.VISIBLE
            }
            HealthConnectSyncState.Syncing, HealthConnectSyncState.Loading -> {
                statusView.setText(R.string.health_connect_syncing)
                statusView.visibility = View.VISIBLE
                connectButton.visibility = View.GONE
            }
            HealthConnectSyncState.Error -> {
                statusView.setText(R.string.health_connect_error)
                statusView.visibility = View.VISIBLE
                connectButton.visibility = View.VISIBLE
            }
            HealthConnectSyncState.Ready -> {
                statusView.visibility = View.GONE
                connectButton.visibility = View.GONE
            }
        }
    }
}
