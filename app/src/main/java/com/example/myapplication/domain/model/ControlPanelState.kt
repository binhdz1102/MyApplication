package com.example.myapplication.domain.model

data class ControlPanelState(
    val rating: Int = 5,
    val temperature: Int = 10,
    val toggledOn: Boolean = false,
    val isServiceConnected: Boolean = false,
)
