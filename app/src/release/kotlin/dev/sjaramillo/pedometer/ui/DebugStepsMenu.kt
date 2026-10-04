package dev.sjaramillo.pedometer.ui

import android.view.Menu
import android.view.MenuItem
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@Suppress("UNUSED_PARAMETER")
class DebugStepsMenu @Inject constructor() {
    fun addTo(menu: Menu) = Unit

    fun select(
        item: MenuItem,
        scope: CoroutineScope,
        invalidate: () -> Unit,
    ): Boolean = false
}
