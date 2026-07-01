package com.example.myapplication.data.launcher.repository

import com.example.myapplication.core.model.WeatherCondition
import com.example.myapplication.core.model.WeatherInfo
import com.example.myapplication.domain.launcher.repository.WeatherRepository
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FakeWeatherRepository
    @Inject
    constructor() : WeatherRepository {
        override fun observeWeather(): Flow<WeatherInfo> =
            flow {
                while (currentCoroutineContext().isActive) {
                    val condition = WeatherCondition.entries.random()
                    emit(
                        WeatherInfo(
                            temperatureCelsius =
                                when (condition) {
                                    WeatherCondition.SUNNY -> Random.nextInt(29, 36)
                                    WeatherCondition.SUN_WITH_CLOUD -> Random.nextInt(26, 33)
                                    WeatherCondition.CLOUDY -> Random.nextInt(22, 30)
                                    WeatherCondition.RAINY -> Random.nextInt(18, 27)
                                    WeatherCondition.SNOWY -> Random.nextInt(-2, 4)
                                    WeatherCondition.THUNDER -> Random.nextInt(20, 29)
                                },
                            humidityPercent =
                                when (condition) {
                                    WeatherCondition.SUNNY -> Random.nextInt(28, 48)
                                    WeatherCondition.SUN_WITH_CLOUD -> Random.nextInt(40, 62)
                                    WeatherCondition.CLOUDY -> Random.nextInt(52, 74)
                                    WeatherCondition.RAINY -> Random.nextInt(75, 93)
                                    WeatherCondition.SNOWY -> Random.nextInt(60, 84)
                                    WeatherCondition.THUNDER -> Random.nextInt(78, 96)
                                },
                            condition = condition,
                            locationLabel = "Hanoi",
                        ),
                    )
                    delay(Random.nextLong(5_000L, 10_001L))
                }
            }
    }
