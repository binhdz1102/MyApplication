package com.example.feature.carproperty.domain.model

data class CarPropertyReading<T : Any>(
    val templateName: String,
    val value: T?,
    val status: CarPropertyStatus,
    val timestampNanos: Long,
    val propertyId: Int,
    val areaId: Int,
    val errorCode: Int? = null,
)
