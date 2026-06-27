package com.example.feature.carproperty.domain.repository

import com.example.feature.carproperty.domain.model.CarPropertyReading
import com.example.feature.carproperty.domain.model.CarPropertyTemplate
import kotlinx.coroutines.flow.Flow

interface CarPropertyRepository {
    suspend fun <T : Any> readOnce(template: CarPropertyTemplate<T>): CarPropertyReading<T>

    fun <T : Any> observe(template: CarPropertyTemplate<T>): Flow<CarPropertyReading<T>>

    suspend fun <T : Any> set(template: CarPropertyTemplate<T>, value: T)
}
