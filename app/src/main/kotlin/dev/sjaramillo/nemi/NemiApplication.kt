package dev.sjaramillo.nemi

import android.app.Application
import dev.sjaramillo.nemi.di.NemiGraph
import dev.zacsweers.metro.createGraphFactory

class NemiApplication : Application() {
    internal val graph by lazy { createGraphFactory<NemiGraph.Factory>().create(this) }
}
