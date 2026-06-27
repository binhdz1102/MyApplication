package com.example.feature.carproperty.presentation.model

import com.example.feature.carproperty.domain.model.CarPropertyTemplate

data class HvacTemplateUiState(
    val propertyLabel: String = CarPropertyTemplate.HvacTemperatureSetRow1Left.propertyLabel,
    val areaLabel: String = CarPropertyTemplate.HvacTemperatureSetRow1Left.areaLabel,
    val inputCelsius: String = "21.0",
    val currentValue: String = "--",
    val currentStatus: String = "Idle",
    val currentTimestampNanos: String = "--",
    val propertyIdHex: String = "--",
    val areaIdHex: String = "--",
    val lastAction: String = "Ready",
    val isObserving: Boolean = false,
    val isBusy: Boolean = false,
)
