package com.example.feature.carproperty.domain.model

import kotlin.jvm.javaObjectType

data class CarPropertyTemplate<T : Any>(
    val propertyClassName: String,
    val propertyFieldName: String,
    val areaClassName: String,
    val areaFieldName: String,
    val valueClass: Class<T>,
    val displayName: String,
    val propertyLabel: String,
    val areaLabel: String,
) {
    companion object {
        val HvacTemperatureSetRow1Left = CarPropertyTemplate(
            propertyClassName = "android.car.VehiclePropertyIds",
            propertyFieldName = "HVAC_TEMPERATURE_SET",
            areaClassName = "android.car.VehicleAreaSeat",
            areaFieldName = "SEAT_ROW_1_LEFT",
            valueClass = Float::class.javaObjectType,
            displayName = "HVAC_TEMPERATURE_SET / ROW_1_LEFT",
            propertyLabel = "HVAC_TEMPERATURE_SET",
            areaLabel = "ROW_1_LEFT",
        )
    }
}
