package com.example.myapplication.feature.launcher.media

import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.common.udp.BaseViewModel
import com.example.myapplication.core.common.udp.UiEvent
import com.example.myapplication.core.common.udp.UiState
import com.example.myapplication.domain.launcher.usecase.ObserveMediaPlaybackUseCase
import com.example.myapplication.domain.launcher.usecase.PlayNextTrackUseCase
import com.example.myapplication.domain.launcher.usecase.PlayPreviousTrackUseCase
import com.example.myapplication.domain.launcher.usecase.SeekToPositionUseCase
import com.example.myapplication.domain.launcher.usecase.ToggleMediaPlaybackUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MediaCardUiState(
    val songName: String = "",
    val appName: String = "",
    val durationSeconds: Int = 1,
    val positionSeconds: Int = 0,
    val isPlaying: Boolean = false,
) : UiState

sealed interface MediaCardUiEvent : UiEvent {
    data object Observe : MediaCardUiEvent

    data object TogglePlayback : MediaCardUiEvent

    data object NextTrack : MediaCardUiEvent

    data object PreviousTrack : MediaCardUiEvent

    data class SeekTo(
        val positionSeconds: Int,
    ) : MediaCardUiEvent
}

@HiltViewModel
class MediaCardViewModel
    @Inject
    constructor(
        private val observeMediaPlaybackUseCase: ObserveMediaPlaybackUseCase,
        private val toggleMediaPlaybackUseCase: ToggleMediaPlaybackUseCase,
        private val playNextTrackUseCase: PlayNextTrackUseCase,
        private val playPreviousTrackUseCase: PlayPreviousTrackUseCase,
        private val seekToPositionUseCase: SeekToPositionUseCase,
    ) : BaseViewModel<MediaCardUiState, MediaCardUiEvent>(MediaCardUiState()) {
        private var observing = false

        override fun handleEvent(event: MediaCardUiEvent) {
            when (event) {
                MediaCardUiEvent.Observe -> startObserving()
                MediaCardUiEvent.TogglePlayback -> viewModelScope.launch { toggleMediaPlaybackUseCase() }
                MediaCardUiEvent.NextTrack -> viewModelScope.launch { playNextTrackUseCase() }
                MediaCardUiEvent.PreviousTrack -> viewModelScope.launch { playPreviousTrackUseCase() }
                is MediaCardUiEvent.SeekTo -> viewModelScope.launch { seekToPositionUseCase(event.positionSeconds) }
            }
        }

        private fun startObserving() {
            if (observing) return
            observing = true
            viewModelScope.launch {
                observeMediaPlaybackUseCase().collect { playback ->
                    updateState {
                        copy(
                            songName = playback.track.songName,
                            appName = playback.track.appName,
                            durationSeconds = playback.track.durationSeconds,
                            positionSeconds = playback.currentPositionSeconds,
                            isPlaying = playback.isPlaying,
                        )
                    }
                }
            }
        }
    }
