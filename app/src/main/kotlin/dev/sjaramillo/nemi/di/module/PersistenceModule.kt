package dev.sjaramillo.nemi.di.module

import android.content.Context
import android.content.SharedPreferences
import androidx.health.connect.client.HealthConnectClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.sjaramillo.nemi.data.NemiDatabase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class PersistenceModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): NemiDatabase = NemiDatabase.getInstance(context)

    @Provides
    @Singleton
    fun provideHealthConnectClient(
        @ApplicationContext context: Context,
    ): HealthConnectClient = HealthConnectClient.getOrCreate(context)

    @Provides
    @Singleton
    fun provideStepsPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = context.getSharedPreferences("nemi", Context.MODE_PRIVATE)
}
