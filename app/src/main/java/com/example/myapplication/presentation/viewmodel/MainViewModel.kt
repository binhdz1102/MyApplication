package com.example.myapplication.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.domain.model.ControlPanelState
import com.example.myapplication.domain.repository.ControlPanelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: ControlPanelRepository,
) : ViewModel() {

    val controlState: StateFlow<ControlPanelState> = repository.controlState

    fun connect() {
        repository.connect()
    }

    fun disconnect() {
        repository.disconnect()
    }

    fun decreaseRating() {
        viewModelScope.launch {
            repository.decreaseRating()
        }
    }

    fun increaseRating() {
        viewModelScope.launch {
            repository.increaseRating()
        }
    }

    fun decreaseTemperature() {
        viewModelScope.launch {
            repository.decreaseTemperature()
        }
    }

    fun increaseTemperature() {
        viewModelScope.launch {
            repository.increaseTemperature()
        }
    }

    fun toggleCenterButton() {
        viewModelScope.launch {
            repository.toggleCenterButton()
        }
    }

    override fun onCleared() {
        repository.disconnect()
        super.onCleared()
    }
}
