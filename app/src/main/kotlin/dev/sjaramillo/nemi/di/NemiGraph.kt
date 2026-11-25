package dev.sjaramillo.nemi.di

import android.content.Context
import android.content.SharedPreferences
import dev.sjaramillo.nemi.data.NemiDatabase
import dev.sjaramillo.nemi.ui.HomeViewModel
import dev.sjaramillo.nemi.ui.SettingsViewModel
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn

@DependencyGraph(AppScope::class)
internal interface NemiGraph {
    val homeViewModel: HomeViewModel
    val settingsViewModel: SettingsViewModel

    @Provides
    @SingleIn(AppScope::class)
    fun provideDatabase(context: Context): NemiDatabase = NemiDatabase.getInstance(context)

    @Provides
    @SingleIn(AppScope::class)
    fun providePreferences(context: Context): SharedPreferences = context.getSharedPreferences("nemi", Context.MODE_PRIVATE)

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides context: Context,
        ): NemiGraph
    }
}
