package com.example.feature.carproperty.di

import com.example.feature.carproperty.data.gateway.CarPropertyGateway
import com.example.feature.carproperty.data.gateway.ReflectionCarPropertyGateway
import com.example.feature.carproperty.data.repository.CarPropertyRepositoryImpl
import com.example.feature.carproperty.domain.repository.CarPropertyRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CarPropertyBindingsModule {

    @Binds
    @Singleton
    abstract fun bindCarPropertyGateway(
        implementation: ReflectionCarPropertyGateway,
    ): CarPropertyGateway

    @Binds
    @Singleton
    abstract fun bindCarPropertyRepository(
        implementation: CarPropertyRepositoryImpl,
    ): CarPropertyRepository
}
