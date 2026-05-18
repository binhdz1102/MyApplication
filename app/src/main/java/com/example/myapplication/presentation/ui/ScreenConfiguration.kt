package com.example.myapplication.presentation.ui

import androidx.annotation.StringRes

data class ScreenConfiguration(
    @StringRes val titleRes: Int,
    val showBackButton: Boolean,
    val showFab: Boolean,
    val showRefresh: Boolean,
)

interface ScreenConfigurationHost {
    fun updateScreenConfiguration(configuration: ScreenConfiguration)
}

interface UserActionHandler {
    fun onAddRequested()

    fun onRefreshRequested()
}
