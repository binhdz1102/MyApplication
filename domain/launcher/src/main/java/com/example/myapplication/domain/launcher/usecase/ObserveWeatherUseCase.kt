package com.example.myapplication.domain.launcher.usecase

import com.example.myapplication.core.model.WeatherInfo
import com.example.myapplication.domain.launcher.repository.WeatherRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveWeatherUseCase
    @Inject
    constructor(
        private val weatherRepository: WeatherRepository,
    ) {
        operator fun invoke(): Flow<WeatherInfo> = weatherRepository.observeWeather()
    }
