package dev.sjaramillo.pedometer.ui

import android.content.Context
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
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.HealthConnectSyncState
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.util.DateUtil
import dev.sjaramillo.pedometer.util.FormatUtil
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.eazegraph.lib.charts.BarChart
import org.eazegraph.lib.charts.PieChart
import org.eazegraph.lib.models.BarModel
import org.eazegraph.lib.models.PieModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.roundToLong

@AndroidEntryPoint
class HomeFragment : Fragment() {
    @Inject
    lateinit var stepsRepository: StepsRepository

    @Inject
    lateinit var healthConnectSyncCoordinator: HealthConnectSyncCoordinator

    private val homeViewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(stepsRepository)
    }

    private lateinit var stepsView: TextView
    private lateinit var sliceGoal: PieModel
    private lateinit var sliceCurrent: PieModel
    private lateinit var graph: PieChart
    private lateinit var statusView: TextView
    private lateinit var connectButton: Button
    private var goal = 0
    private var showSteps = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        inflater.inflate(R.layout.fragment_home, container, false).also { view ->
            stepsView = view.findViewById(R.id.steps)
            graph = view.findViewById(R.id.graph)
            statusView = view.findViewById(R.id.health_connect_status)
            connectButton = view.findViewById(R.id.connect_health_connect)
            connectButton.setOnClickListener {
                (activity as? MainActivity)?.requestHealthConnectAccess()
            }

            sliceCurrent = PieModel("", 0f, Color.parseColor("#99CC00"))
            sliceGoal =
                PieModel("", SettingsFragment.DEFAULT_GOAL.toFloat(), Color.parseColor("#CC0000"))
            graph.addPieSlice(sliceCurrent)
            graph.addPieSlice(sliceGoal)
            graph.setOnClickListener {
                showSteps = !showSteps
                renderStepData(homeViewModel.entries.value)
            }
            graph.isDrawValueInPie = false
            graph.isUsePieRotation = true
            graph.startAnimation()
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    healthConnectSyncCoordinator.state.collect(::updateHealthConnectState)
                }
                launch {
                    homeViewModel.entries.collect { entries ->
                        renderStepData(entries)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
        goal = prefs.getInt("goal", SettingsFragment.DEFAULT_GOAL)
        renderStepData(homeViewModel.entries.value)
    }

    private fun renderUnit() {
        if (!::stepsView.isInitialized) return
        val unit =
            if (showSteps) {
                getString(R.string.steps)
            } else if (
                requireContext()
                    .getSharedPreferences("pedometer", Context.MODE_PRIVATE)
                    .getString("step_size_unit", SettingsFragment.DEFAULT_STEP_UNIT) == "cm"
            ) {
                getString(R.string.distance_unit_km)
            } else {
                getString(R.string.distance_unit_mi)
            }
        requireView().findViewById<TextView>(R.id.unit).text = unit
    }

    private fun updatePie(stepsToday: Long) {
        sliceCurrent.value = stepsToday.toFloat()
        if (goal > stepsToday) {
            if (graph.data.size == 1) graph.addPieSlice(sliceGoal)
            sliceGoal.value = (goal - stepsToday).toFloat()
        } else {
            graph.clearChart()
            graph.addPieSlice(sliceCurrent)
        }
        graph.update()

        val numberFormat = FormatUtil.numberFormat
        if (showSteps) {
            stepsView.text = numberFormat.format(stepsToday)
        } else {
            val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
            val stepSize = prefs.getFloat("step_size_value", SettingsFragment.DEFAULT_STEP_SIZE)
            var distanceToday = stepsToday * stepSize
            distanceToday /=
                if (prefs.getString("step_size_unit", SettingsFragment.DEFAULT_STEP_UNIT) == "cm") {
                    100000f
                } else {
                    5280f
                }
            stepsView.text = numberFormat.format(distanceToday.toDouble())
        }
    }

    private fun renderStepData(entries: List<DailySteps>) {
        renderUnit()
        updatePie(entries.firstOrNull { it.day == LocalDate.now().toEpochDay() }?.steps ?: 0)
        updateBars(entries.takeLast(8).asReversed())
    }

    private fun updateBars(lastEntries: List<DailySteps>) {
        val barChart = requireView().findViewById<BarChart>(R.id.bargraph)
        if (barChart.data.isNotEmpty()) barChart.clearChart()
        val prefs = requireContext().getSharedPreferences("pedometer", Context.MODE_PRIVATE)
        val stepSize = prefs.getFloat("step_size_value", SettingsFragment.DEFAULT_STEP_SIZE)
        val stepSizeCm = prefs.getString("step_size_unit", SettingsFragment.DEFAULT_STEP_UNIT) == "cm"
        barChart.isShowDecimal = !showSteps

        lastEntries.asReversed().dropLast(1).forEach { current ->
            if (current.steps > 0) {
                val value =
                    if (showSteps) {
                        current.steps.toFloat()
                    } else {
                        var distance = current.steps * stepSize
                        distance /= if (stepSizeCm) 100000f else 5280f
                        (distance * 1000).roundToLong() / 1000f
                    }
                barChart.addBar(
                    BarModel(
                        DateTimeFormatter.ofPattern("E").format(DateUtil.dayToLocalDate(current.day)),
                        value,
                        if (current.steps > goal) Color.parseColor("#99CC00") else Color.parseColor("#0099cc"),
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
                statusView.setText(
                    if (healthConnectSyncCoordinator.isFake) R.string.fake_steps_error else R.string.health_connect_error,
                )
                statusView.visibility = View.VISIBLE
                connectButton.visibility = if (healthConnectSyncCoordinator.isFake) View.GONE else View.VISIBLE
            }
            HealthConnectSyncState.Unavailable -> {
                statusView.setText(R.string.health_connect_unavailable)
                statusView.visibility = View.VISIBLE
                connectButton.visibility = View.GONE
            }
            HealthConnectSyncState.Ready -> {
                statusView.setText(R.string.fake_steps_enabled)
                statusView.visibility = if (healthConnectSyncCoordinator.isFake) View.VISIBLE else View.GONE
                connectButton.visibility = View.GONE
            }
        }
    }
}
