package dev.sjaramillo.pedometer.di.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.sjaramillo.pedometer.data.PhoneStepsDataSource
import dev.sjaramillo.pedometer.data.StepsProvider

@Module
@InstallIn(SingletonComponent::class)
abstract class StepsProviderModule {
    @Binds
    abstract fun bindStepsProvider(provider: PhoneStepsDataSource): StepsProvider
}
