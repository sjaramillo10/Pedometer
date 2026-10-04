package dev.sjaramillo.pedometer.ui

import android.view.Menu
import android.view.MenuItem
import dev.sjaramillo.pedometer.R
import dev.sjaramillo.pedometer.data.DebugStepsProvider
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

class DebugStepsMenu @Inject constructor(
    private val provider: DebugStepsProvider,
    private val coordinator: HealthConnectSyncCoordinator,
) {
    fun addTo(menu: Menu) {
        menu.addSubMenu(R.string.debug_menu).add(Menu.NONE, R.id.action_fake_steps, Menu.NONE, R.string.use_fake_steps).apply {
            isCheckable = true
            isChecked = provider.isFake
        }
    }

    fun select(
        item: MenuItem,
        scope: CoroutineScope,
        invalidate: () -> Unit,
    ): Boolean {
        if (item.itemId != R.id.action_fake_steps) return false
        val enabled = !provider.isFake
        scope.launch {
            coordinator.refreshAfter { provider.setFakeSteps(enabled) }
            invalidate()
        }
        return true
    }
}
