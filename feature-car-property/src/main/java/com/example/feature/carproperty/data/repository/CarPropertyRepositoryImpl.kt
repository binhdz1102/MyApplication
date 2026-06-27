package com.example.feature.carproperty.data.repository

import com.example.feature.carproperty.data.gateway.CarPropertyGateway
import com.example.feature.carproperty.domain.exception.CarPropertyAccessException
import com.example.feature.carproperty.domain.model.CarPropertyReading
import com.example.feature.carproperty.domain.model.CarPropertyTemplate
import com.example.feature.carproperty.domain.repository.CarPropertyRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

@Singleton
class CarPropertyRepositoryImpl @Inject constructor(
    private val gateway: CarPropertyGateway,
) : CarPropertyRepository {

    override suspend fun <T : Any> readOnce(template: CarPropertyTemplate<T>): CarPropertyReading<T> {
        return runOperation { gateway.readOnce(template) }
    }

    override fun <T : Any> observe(template: CarPropertyTemplate<T>): Flow<CarPropertyReading<T>> {
        return gateway.observe(template).catch { throwable ->
            throw throwable.toDomainException()
        }
    }

    override suspend fun <T : Any> set(template: CarPropertyTemplate<T>, value: T) {
        runOperation { gateway.set(template, value) }
    }

    private suspend fun <T> runOperation(block: suspend () -> T): T {
        return try {
            block()
        } catch (throwable: Throwable) {
            throw throwable.toDomainException()
        }
    }

    private fun Throwable.toDomainException(): CarPropertyAccessException {
        return if (this is CarPropertyAccessException) {
            this
        } else {
            CarPropertyAccessException(
                message = message ?: "Car property operation failed.",
                cause = this,
            )
        }
    }
}
