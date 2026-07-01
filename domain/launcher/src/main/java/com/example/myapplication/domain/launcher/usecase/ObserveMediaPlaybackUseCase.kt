package com.example.myapplication.domain.launcher.usecase

import com.example.myapplication.core.model.MediaPlayback
import com.example.myapplication.domain.launcher.repository.MediaRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveMediaPlaybackUseCase
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
    ) {
        operator fun invoke(): Flow<MediaPlayback> = mediaRepository.observePlayback()
    }
