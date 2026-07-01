package com.example.myapplication.data.launcher.repository

import com.example.myapplication.core.model.MediaPlayback
import com.example.myapplication.core.model.MediaTrack
import com.example.myapplication.domain.launcher.repository.MediaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FakeMediaRepository
    @Inject
    constructor() : MediaRepository {
        private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        private val playlist =
            listOf(
                MediaTrack("track_1", "Neon Highway", "Spotify", 214),
                MediaTrack("track_2", "Rain Over City", "YouTube Music", 187),
                MediaTrack("track_3", "Morning Grid", "Pocket Casts", 265),
                MediaTrack("track_4", "Analog Hearts", "Deezer", 203),
            )
        private val playbackState =
            MutableStateFlow(
                MediaPlayback(
                    track = playlist.first(),
                    isPlaying = true,
                    currentPositionSeconds = 38,
                ),
            )

        init {
            repositoryScope.launch {
                while (isActive) {
                    delay(1_000L)
                    playbackState.update { current ->
                        if (!current.isPlaying) {
                            current
                        } else {
                            val nextPosition = current.currentPositionSeconds + 1
                            if (nextPosition >= current.track.durationSeconds) {
                                nextTrack(current.track.id)
                            } else {
                                current.copy(currentPositionSeconds = nextPosition)
                            }
                        }
                    }
                }
            }
        }

        override fun observePlayback(): Flow<MediaPlayback> = playbackState.asStateFlow()

        override suspend fun togglePlayback() {
            playbackState.update { it.copy(isPlaying = !it.isPlaying) }
        }

        override suspend fun playNext() {
            playbackState.update { current -> nextTrack(current.track.id) }
        }

        override suspend fun playPrevious() {
            playbackState.update { current -> previousTrack(current.track.id) }
        }

        override suspend fun seekTo(positionSeconds: Int) {
            playbackState.update { current ->
                current.copy(
                    currentPositionSeconds = positionSeconds.coerceIn(0, current.track.durationSeconds),
                )
            }
        }

        private fun nextTrack(currentId: String): MediaPlayback {
            val currentIndex = playlist.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
            val next = playlist[(currentIndex + 1) % playlist.size]
            return MediaPlayback(track = next, isPlaying = true, currentPositionSeconds = 0)
        }

        private fun previousTrack(currentId: String): MediaPlayback {
            val currentIndex = playlist.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
            val previous = if (currentIndex == 0) playlist.last() else playlist[currentIndex - 1]
            return MediaPlayback(track = previous, isPlaying = true, currentPositionSeconds = 0)
        }
    }
