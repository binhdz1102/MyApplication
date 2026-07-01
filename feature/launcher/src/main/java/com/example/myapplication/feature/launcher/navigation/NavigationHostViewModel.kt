package com.example.myapplication.feature.launcher.navigation

import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.common.udp.BaseViewModel
import com.example.myapplication.core.common.udp.UiEvent
import com.example.myapplication.core.common.udp.UiState
import com.example.myapplication.core.model.AppShortcut
import com.example.myapplication.core.model.NavigationCardInfo
import com.example.myapplication.domain.launcher.usecase.GetAppPagesUseCase
import com.example.myapplication.domain.launcher.usecase.GetNavigationCardInfoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NavigationHostUiState(
    val activeSurface: NavigationSurface = NavigationSurface.NAVIGATION,
    val navigationCard: NavigationCardInfo? = null,
    val appPages: List<List<AppShortcut>> = emptyList(),
    val currentAppPage: Int = 0,
    val isLoading: Boolean = true,
) : UiState

sealed interface NavigationHostUiEvent : UiEvent {
    data object Initialize : NavigationHostUiEvent

    data object ShowNavigation : NavigationHostUiEvent

    data object ShowAppList : NavigationHostUiEvent

    data class UpdateAppPage(
        val index: Int,
    ) : NavigationHostUiEvent
}

@HiltViewModel
class NavigationHostViewModel
    @Inject
    constructor(
        private val getNavigationCardInfoUseCase: GetNavigationCardInfoUseCase,
        private val getAppPagesUseCase: GetAppPagesUseCase,
    ) : BaseViewModel<NavigationHostUiState, NavigationHostUiEvent>(NavigationHostUiState()) {
        private var hasLoaded = false

        override fun handleEvent(event: NavigationHostUiEvent) {
            when (event) {
                NavigationHostUiEvent.Initialize -> initialize()
                NavigationHostUiEvent.ShowAppList -> updateState { copy(activeSurface = NavigationSurface.APPS) }
                NavigationHostUiEvent.ShowNavigation -> updateState { copy(activeSurface = NavigationSurface.NAVIGATION) }
                is NavigationHostUiEvent.UpdateAppPage -> updateState { copy(currentAppPage = event.index) }
            }
        }

        private fun initialize() {
            if (hasLoaded) return
            hasLoaded = true

            viewModelScope.launch {
                val navigationCard = getNavigationCardInfoUseCase()
                val appPages = getAppPagesUseCase()
                updateState {
                    copy(
                        navigationCard = navigationCard,
                        appPages = appPages,
                        isLoading = false,
                    )
                }
            }
        }
    }
