package com.example.myapplication.core.common.udp

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

abstract class BaseViewModel<S : UiState, E : UiEvent>(
    initialState: S,
) : ViewModel() {
    private val _uiState = MutableStateFlow(initialState)
    val uiState: StateFlow<S> = _uiState.asStateFlow()

    fun onEvent(event: E) {
        handleEvent(event)
    }

    protected fun updateState(reducer: S.() -> S) {
        _uiState.update(reducer)
    }

    protected abstract fun handleEvent(event: E)
}
