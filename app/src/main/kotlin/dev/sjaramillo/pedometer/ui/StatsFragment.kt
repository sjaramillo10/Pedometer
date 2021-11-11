package dev.sjaramillo.pedometer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.StepsRepository
import javax.inject.Inject

@AndroidEntryPoint
class StatsFragment : Fragment() {

    @Inject
    lateinit var stepsRepository: StepsRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        return view
    }
}
