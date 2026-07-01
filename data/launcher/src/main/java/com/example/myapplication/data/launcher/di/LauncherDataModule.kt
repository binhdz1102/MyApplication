package com.example.myapplication.data.launcher.di

import com.example.myapplication.data.launcher.repository.FakeDateRepository
import com.example.myapplication.data.launcher.repository.FakeLauncherRepository
import com.example.myapplication.data.launcher.repository.FakeMediaRepository
import com.example.myapplication.data.launcher.repository.FakeWeatherRepository
import com.example.myapplication.domain.launcher.repository.DateRepository
import com.example.myapplication.domain.launcher.repository.LauncherRepository
import com.example.myapplication.domain.launcher.repository.MediaRepository
import com.example.myapplication.domain.launcher.repository.WeatherRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LauncherDataModule {
    @Binds
    @Singleton
    abstract fun bindsLauncherRepository(impl: FakeLauncherRepository): LauncherRepository

    @Binds
    @Singleton
    abstract fun bindsDateRepository(impl: FakeDateRepository): DateRepository

    @Binds
    @Singleton
    abstract fun bindsWeatherRepository(impl: FakeWeatherRepository): WeatherRepository

    @Binds
    @Singleton
    abstract fun bindsMediaRepository(impl: FakeMediaRepository): MediaRepository
}
