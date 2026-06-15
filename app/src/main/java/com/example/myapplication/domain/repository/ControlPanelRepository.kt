package com.example.myapplication.domain.repository

import com.example.myapplication.domain.model.ControlPanelState
import kotlinx.coroutines.flow.StateFlow

interface ControlPanelRepository {
    val controlState: StateFlow<ControlPanelState>

    fun connect()

    fun disconnect()

    suspend fun decreaseRating()

    suspend fun increaseRating()

    suspend fun decreaseTemperature()

    suspend fun increaseTemperature()

    suspend fun toggleCenterButton()
}
