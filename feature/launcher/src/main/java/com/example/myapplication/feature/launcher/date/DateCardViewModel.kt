package com.example.myapplication.feature.launcher.date

import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.common.udp.BaseViewModel
import com.example.myapplication.core.common.udp.UiEvent
import com.example.myapplication.core.common.udp.UiState
import com.example.myapplication.domain.launcher.usecase.ObserveDateCardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DateCardUiState(
    val dayOfWeek: String = "",
    val dayOfMonth: String = "",
    val monthYear: String = "",
    val fullDateLabel: String = "",
) : UiState

sealed interface DateCardUiEvent : UiEvent {
    data object Observe : DateCardUiEvent
}

@HiltViewModel
class DateCardViewModel
    @Inject
    constructor(
        private val observeDateCardUseCase: ObserveDateCardUseCase,
    ) : BaseViewModel<DateCardUiState, DateCardUiEvent>(DateCardUiState()) {
        private var observing = false

        override fun handleEvent(event: DateCardUiEvent) {
            if (event is DateCardUiEvent.Observe && !observing) {
                observing = true
                viewModelScope.launch {
                    observeDateCardUseCase().collect { dateInfo ->
                        updateState {
                            copy(
                                dayOfWeek = dateInfo.dayOfWeek,
                                dayOfMonth = dateInfo.dayOfMonth,
                                monthYear = dateInfo.monthYear,
                                fullDateLabel = dateInfo.fullDateLabel,
                            )
                        }
                    }
                }
            }
        }
    }
