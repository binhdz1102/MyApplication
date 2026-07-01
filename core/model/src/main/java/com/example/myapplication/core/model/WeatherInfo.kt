package com.example.myapplication.core.model

data class WeatherInfo(
    val temperatureCelsius: Int,
    val humidityPercent: Int,
    val condition: WeatherCondition,
    val locationLabel: String,
)
