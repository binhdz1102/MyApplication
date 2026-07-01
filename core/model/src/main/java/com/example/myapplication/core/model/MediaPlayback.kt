package com.example.myapplication.core.model

data class MediaPlayback(
    val track: MediaTrack,
    val isPlaying: Boolean,
    val currentPositionSeconds: Int,
)
