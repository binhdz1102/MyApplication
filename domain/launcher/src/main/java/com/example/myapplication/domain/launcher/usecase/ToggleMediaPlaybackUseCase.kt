package com.example.myapplication.domain.launcher.usecase

import com.example.myapplication.domain.launcher.repository.MediaRepository
import javax.inject.Inject

class ToggleMediaPlaybackUseCase
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
    ) {
        suspend operator fun invoke() = mediaRepository.togglePlayback()
    }
