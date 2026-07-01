package com.example.myapplication.domain.launcher.repository

import com.example.myapplication.core.model.MediaPlayback
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun observePlayback(): Flow<MediaPlayback>

    suspend fun togglePlayback()

    suspend fun playNext()

    suspend fun playPrevious()

    suspend fun seekTo(positionSeconds: Int)
}
