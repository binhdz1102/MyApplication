package com.example.feature.carproperty.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.feature.carproperty.domain.model.CarPropertyReading
import com.example.feature.carproperty.domain.model.CarPropertyStatus
import com.example.feature.carproperty.domain.model.CarPropertyTemplate
import com.example.feature.carproperty.domain.usecase.ObserveCarPropertyUseCase
import com.example.feature.carproperty.domain.usecase.ReadCarPropertyUseCase
import com.example.feature.carproperty.domain.usecase.SetCarPropertyUseCase
import com.example.feature.carproperty.presentation.model.HvacTemplateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HvacTemplateViewModel @Inject constructor(
    private val readCarPropertyUseCase: ReadCarPropertyUseCase,
    private val observeCarPropertyUseCase: ObserveCarPropertyUseCase,
    private val setCarPropertyUseCase: SetCarPropertyUseCase,
) : ViewModel() {

    private val template = CarPropertyTemplate.HvacTemperatureSetRow1Left

    private val _uiState = MutableStateFlow(HvacTemplateUiState())
    val uiState: StateFlow<HvacTemplateUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private var observationJob: Job? = null

    fun onTemperatureInputChanged(input: String) {
        _uiState.update { currentState ->
            currentState.copy(inputCelsius = input)
        }
    }

    fun readOnce() {
        viewModelScope.launch {
            _uiState.update { currentState -> currentState.copy(isBusy = true, lastAction = "readOnce") }
            try {
                val reading = readCarPropertyUseCase(template)
                applyReading(reading, source = "readOnce")
            } catch (throwable: Throwable) {
                handleError("Read once failed", throwable)
            } finally {
                _uiState.update { currentState -> currentState.copy(isBusy = false) }
            }
        }
    }

    fun toggleObservation() {
        if (observationJob == null) {
            startObservation()
        } else {
            stopObservation()
        }
    }

    fun setTemperature() {
        val parsedValue = _uiState.value.inputCelsius.toFloatOrNull()
        if (parsedValue == null) {
            _messages.tryEmit("Gia tri nhiet do khong hop le. Hay nhap so thap phan, vi du 21.5")
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    lastAction = String.format(Locale.US, "set(%.1f C)", parsedValue),
                )
            }
            try {
                setCarPropertyUseCase(template, parsedValue)
                val reading = readCarPropertyUseCase(template)
                applyReading(reading, source = String.format(Locale.US, "set %.1f C", parsedValue))
                _messages.tryEmit(String.format(Locale.US, "Da gui lenh set %.1f C", parsedValue))
            } catch (throwable: Throwable) {
                handleError("Set property failed", throwable)
            } finally {
                _uiState.update { currentState -> currentState.copy(isBusy = false) }
            }
        }
    }

    private fun startObservation() {
        if (observationJob != null) {
            return
        }

        observationJob = viewModelScope.launch {
            _uiState.update { currentState ->
                currentState.copy(isObserving = true, lastAction = "observe started")
            }
            _messages.tryEmit("Bat dau observe HVAC_TEMPERATURE_SET / ROW_1_LEFT")
            try {
                observeCarPropertyUseCase(template).collect { reading ->
                    applyReading(reading, source = "observe")
                }
            } catch (throwable: Throwable) {
                handleError("Observe failed", throwable)
            } finally {
                observationJob = null
                _uiState.update { currentState -> currentState.copy(isObserving = false) }
            }
        }
    }

    private fun stopObservation() {
        observationJob?.cancel()
        observationJob = null
        _uiState.update { currentState ->
            currentState.copy(isObserving = false, lastAction = "observe stopped")
        }
        _messages.tryEmit("Da dung observe")
    }

    private fun applyReading(reading: CarPropertyReading<Float>, source: String) {
        _uiState.update { currentState ->
            currentState.copy(
                currentValue = reading.value?.let { String.format(Locale.US, "%.1f C", it) } ?: "--",
                currentStatus = buildStatusLabel(reading),
                currentTimestampNanos = reading.timestampNanos.toString(),
                propertyIdHex = toHex(reading.propertyId),
                areaIdHex = toHex(reading.areaId),
                lastAction = source,
            )
        }
    }

    private fun buildStatusLabel(reading: CarPropertyReading<Float>): String {
        return when (reading.status) {
            CarPropertyStatus.AVAILABLE -> "Available"
            CarPropertyStatus.UNAVAILABLE -> "Unavailable"
            CarPropertyStatus.ERROR -> reading.errorCode?.let { "Error($it)" } ?: "Error"
        }
    }

    private fun toHex(value: Int): String {
        return "0x" + Integer.toHexString(value)
    }

    private fun handleError(prefix: String, throwable: Throwable) {
        val message = throwable.message ?: throwable.cause?.message ?: "Unknown error"
        _uiState.update { currentState ->
            currentState.copy(
                currentStatus = "Error",
                lastAction = prefix,
            )
        }
        _messages.tryEmit("$prefix: $message")
    }
}

