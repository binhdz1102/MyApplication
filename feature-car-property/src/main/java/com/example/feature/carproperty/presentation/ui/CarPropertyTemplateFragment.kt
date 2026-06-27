package com.example.feature.carproperty.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.feature.carproperty.R
import com.example.feature.carproperty.databinding.FragmentCarPropertyTemplateBinding
import com.example.feature.carproperty.presentation.model.HvacTemplateUiState
import com.example.feature.carproperty.presentation.viewmodel.HvacTemplateViewModel
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CarPropertyTemplateFragment : Fragment() {

    private var binding: FragmentCarPropertyTemplateBinding? = null
    private val viewModel: HvacTemplateViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentCarPropertyTemplateBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val currentBinding = binding ?: return

        currentBinding.buttonReadOnce.setOnClickListener {
            viewModel.readOnce()
        }
        currentBinding.buttonToggleObserve.setOnClickListener {
            viewModel.toggleObservation()
        }
        currentBinding.buttonSetTemperature.setOnClickListener {
            viewModel.setTemperature()
        }
        currentBinding.inputSetTemperature.doAfterTextChanged { editable ->
            viewModel.onTemperatureInputChanged(editable?.toString().orEmpty())
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        render(state)
                    }
                }
                launch {
                    viewModel.messages.collect { message ->
                        val root = binding?.root ?: return@collect
                        Snackbar.make(root, message, Snackbar.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun render(state: HvacTemplateUiState) {
        val currentBinding = binding ?: return
        currentBinding.progressLoading.isVisible = state.isBusy
        currentBinding.textPropertyValue.text = state.propertyLabel
        currentBinding.textAreaValue.text = state.areaLabel
        currentBinding.textCurrentValue.text = state.currentValue
        currentBinding.textStatusValue.text = state.currentStatus
        currentBinding.textTimestampValue.text = state.currentTimestampNanos
        currentBinding.textPropertyIdValue.text = state.propertyIdHex
        currentBinding.textAreaIdValue.text = state.areaIdHex
        currentBinding.textLastActionValue.text = state.lastAction
        currentBinding.buttonToggleObserve.setText(
            if (state.isObserving) {
                R.string.car_property_action_stop_observing
            } else {
                R.string.car_property_action_start_observing
            },
        )

        val currentInput = currentBinding.inputSetTemperature.text?.toString().orEmpty()
        if (currentInput != state.inputCelsius) {
            currentBinding.inputSetTemperature.setText(state.inputCelsius)
            currentBinding.inputSetTemperature.setSelection(state.inputCelsius.length)
        }
    }

    companion object {
        const val TAG = "CarPropertyTemplateFragment"

        @JvmStatic
        fun newInstance(): CarPropertyTemplateFragment = CarPropertyTemplateFragment()
    }
}

