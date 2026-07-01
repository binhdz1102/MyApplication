package com.example.myapplication.domain.launcher.repository

import com.example.myapplication.core.model.WeatherInfo
import kotlinx.coroutines.flow.Flow

interface WeatherRepository {
    fun observeWeather(): Flow<WeatherInfo>
}
