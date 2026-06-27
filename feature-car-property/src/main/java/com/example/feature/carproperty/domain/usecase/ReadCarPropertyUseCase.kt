package com.example.feature.carproperty.domain.usecase

import com.example.feature.carproperty.domain.model.CarPropertyReading
import com.example.feature.carproperty.domain.model.CarPropertyTemplate
import com.example.feature.carproperty.domain.repository.CarPropertyRepository
import javax.inject.Inject

class ReadCarPropertyUseCase @Inject constructor(
    private val repository: CarPropertyRepository,
) {
    suspend operator fun <T : Any> invoke(template: CarPropertyTemplate<T>): CarPropertyReading<T> {
        return repository.readOnce(template)
    }
}
