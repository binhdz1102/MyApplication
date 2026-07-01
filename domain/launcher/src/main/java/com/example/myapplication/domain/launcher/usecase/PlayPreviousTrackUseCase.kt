package com.example.myapplication.domain.launcher.usecase

import com.example.myapplication.domain.launcher.repository.MediaRepository
import javax.inject.Inject

class PlayPreviousTrackUseCase
    @Inject
    constructor(
        private val mediaRepository: MediaRepository,
    ) {
        suspend operator fun invoke() = mediaRepository.playPrevious()
    }
