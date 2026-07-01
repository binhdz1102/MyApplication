package com.example.myapplication.feature.launcher.weather

import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.common.udp.BaseViewModel
import com.example.myapplication.core.common.udp.UiEvent
import com.example.myapplication.core.common.udp.UiState
import com.example.myapplication.core.model.WeatherCondition
import com.example.myapplication.domain.launcher.usecase.ObserveWeatherUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class WeatherCardUiState(
    val temperatureText: String = "--",
    val humidityText: String = "--",
    val condition: WeatherCondition = WeatherCondition.SUN_WITH_CLOUD,
    val locationLabel: String = "",
) : UiState

sealed interface WeatherCardUiEvent : UiEvent {
    data object Observe : WeatherCardUiEvent
}

@HiltViewModel
class WeatherCardViewModel
    @Inject
    constructor(
        private val observeWeatherUseCase: ObserveWeatherUseCase,
    ) : BaseViewModel<WeatherCardUiState, WeatherCardUiEvent>(WeatherCardUiState()) {
        private var observing = false

        override fun handleEvent(event: WeatherCardUiEvent) {
            if (event is WeatherCardUiEvent.Observe && !observing) {
                observing = true
                viewModelScope.launch {
                    observeWeatherUseCase().collect { weatherInfo ->
                        updateState {
                            copy(
                                temperatureText = "${weatherInfo.temperatureCelsius}°C",
                                humidityText = "${weatherInfo.humidityPercent}%",
                                condition = weatherInfo.condition,
                                locationLabel = weatherInfo.locationLabel,
                            )
                        }
                    }
                }
            }
        }
    }
