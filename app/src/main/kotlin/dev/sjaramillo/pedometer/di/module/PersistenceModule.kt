package dev.sjaramillo.pedometer.di.module

import android.content.Context
import android.content.SharedPreferences
import androidx.health.connect.client.HealthConnectClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sjaramillo.pedometer.data.DefaultHealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.DefaultStepsRepository
import dev.sjaramillo.pedometer.data.HealthConnectSyncCoordinator
import dev.sjaramillo.pedometer.data.PedometerDatabase
import dev.sjaramillo.pedometer.data.StepsRepository
import dev.sjaramillo.pedometer.ui.home.DefaultHomePreferences
import dev.sjaramillo.pedometer.ui.home.HomePreferences
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PersistenceModule {
    @Binds
    @Singleton
    abstract fun bindStepsRepository(impl: DefaultStepsRepository): StepsRepository

    @Binds
    @Singleton
    abstract fun bindHealthConnectSyncCoordinator(impl: DefaultHealthConnectSyncCoordinator): HealthConnectSyncCoordinator

    @Binds
    @Singleton
    abstract fun bindHomePreferences(impl: DefaultHomePreferences): HomePreferences

    companion object {
        @Provides
        @Singleton
        fun provideDatabase(
            @ApplicationContext context: Context,
        ): PedometerDatabase = PedometerDatabase.getInstance(context)

        @Provides
        @Singleton
        fun provideHealthConnectClient(
            @ApplicationContext context: Context,
        ): HealthConnectClient = HealthConnectClient.getOrCreate(context)

        @Provides
        @Singleton
        fun provideStepsPreferences(
            @ApplicationContext context: Context,
        ): SharedPreferences = context.getSharedPreferences("pedometer", Context.MODE_PRIVATE)
    }
}
