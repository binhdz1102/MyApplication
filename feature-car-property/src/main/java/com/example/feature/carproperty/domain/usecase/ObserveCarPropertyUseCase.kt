package com.example.feature.carproperty.domain.usecase

import com.example.feature.carproperty.domain.model.CarPropertyReading
import com.example.feature.carproperty.domain.model.CarPropertyTemplate
import com.example.feature.carproperty.domain.repository.CarPropertyRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCarPropertyUseCase @Inject constructor(
    private val repository: CarPropertyRepository,
) {
    operator fun <T : Any> invoke(template: CarPropertyTemplate<T>): Flow<CarPropertyReading<T>> {
        return repository.observe(template)
    }
}
