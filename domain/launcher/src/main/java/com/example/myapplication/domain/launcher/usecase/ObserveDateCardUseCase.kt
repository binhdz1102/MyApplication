package com.example.myapplication.domain.launcher.usecase

import com.example.myapplication.core.model.DateCardInfo
import com.example.myapplication.domain.launcher.repository.DateRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveDateCardUseCase
    @Inject
    constructor(
        private val dateRepository: DateRepository,
    ) {
        operator fun invoke(): Flow<DateCardInfo> = dateRepository.observeDateCard()
    }
