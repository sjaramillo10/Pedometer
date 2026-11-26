package dev.sjaramillo.nemi

import android.app.Application
import dev.sjaramillo.nemi.data.HealthConnectStepCacheSynchronizer
import dev.sjaramillo.nemi.data.HealthConnectSyncCoordinator
import dev.sjaramillo.nemi.data.NemiDatabase
import dev.sjaramillo.nemi.data.PhoneStepsDataSource
import dev.sjaramillo.nemi.data.StepsRepository
import dev.sjaramillo.nemi.ui.HomeViewModel
import dev.sjaramillo.nemi.ui.NemiPreferences
import dev.sjaramillo.nemi.ui.SettingsViewModel

class NemiApplication : Application() {
    private val steps by lazy { StepsRepository(NemiDatabase(this)) }
    private val preferences by lazy { NemiPreferences(getSharedPreferences("nemi", MODE_PRIVATE)) }
    private val healthConnect by lazy {
        val source = PhoneStepsDataSource(this)
        HealthConnectSyncCoordinator(source, HealthConnectStepCacheSynchronizer(steps, source))
    }

    internal fun createHomeViewModel(): HomeViewModel = HomeViewModel(preferences, steps, healthConnect)

    internal fun createSettingsViewModel(): SettingsViewModel = SettingsViewModel(this, preferences, steps, healthConnect)
}
