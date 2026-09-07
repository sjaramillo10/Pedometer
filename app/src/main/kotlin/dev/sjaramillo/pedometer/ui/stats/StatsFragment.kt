package dev.sjaramillo.pedometer.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.google.android.material.composethemeadapter.MdcTheme
import dagger.hilt.android.AndroidEntryPoint
import dev.sjaramillo.pedometer.data.StepsRepository
import javax.inject.Inject

@AndroidEntryPoint
class StatsFragment : Fragment() {
    @Inject
    lateinit var stepsRepository: StepsRepository

    private val statsViewModel: StatsViewModel by viewModels {
        StatsViewModelFactory(stepsRepository)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View =
        ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        (view as ComposeView).setContent {
            MdcTheme {
                StatsScreen(statsViewModel)
            }
        }
    }
}
