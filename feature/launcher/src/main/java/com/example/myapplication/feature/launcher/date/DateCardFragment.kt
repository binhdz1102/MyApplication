package com.example.myapplication.feature.launcher.date

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.myapplication.core.ui.ext.collectLatestState
import com.example.myapplication.feature.launcher.databinding.FragmentDateCardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DateCardFragment : Fragment() {
    private var bindingReference: FragmentDateCardBinding? = null
    private val binding get() = checkNotNull(bindingReference)
    private val viewModel: DateCardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingReference = FragmentDateCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onEvent(DateCardUiEvent.Observe)
        collectLatestState(viewModel.uiState) { state ->
            binding.dayOfWeekValue.text = state.dayOfWeek
            binding.dayOfMonthValue.text = state.dayOfMonth
            binding.monthYearValue.text = state.monthYear
            binding.fullDateValue.text = state.fullDateLabel
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingReference = null
    }
}
