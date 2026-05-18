package com.example.myapplication.presentation.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentServiceStatusBinding
import com.example.myapplication.di.AppContainer
import com.example.myapplication.presentation.model.ServiceConnectionUiState
import com.example.myapplication.presentation.viewmodel.UserViewModel

class ServiceStatusFragment : Fragment() {

    private var _binding: FragmentServiceStatusBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: UserViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentServiceStatusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(
            requireActivity(),
            AppContainer.provideUserViewModelFactory(requireContext().applicationContext),
        )[UserViewModel::class.java]

        binding.buttonRetryConnection.setOnClickListener {
            viewModel.connectService()
        }

        viewModel.serviceConnectionState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun renderState(state: ServiceConnectionUiState) {
        when (state) {
            ServiceConnectionUiState.CONNECTING -> {
                binding.textStatusTitle.setText(R.string.service_connecting_title)
                binding.textStatusMessage.setText(R.string.service_connecting_message)
                binding.buttonRetryConnection.setText(R.string.action_connecting)
                binding.buttonRetryConnection.isEnabled = false
            }

            ServiceConnectionUiState.CONNECTED -> {
                binding.textStatusTitle.setText(R.string.service_connected_title)
                binding.textStatusMessage.setText(R.string.service_connected_message)
                binding.buttonRetryConnection.setText(R.string.action_read_users)
                binding.buttonRetryConnection.isEnabled = false
            }

            ServiceConnectionUiState.DISCONNECTED -> {
                binding.textStatusTitle.setText(R.string.service_disconnected_title)
                binding.textStatusMessage.setText(R.string.service_disconnected_message)
                binding.buttonRetryConnection.setText(R.string.action_retry_connection)
                binding.buttonRetryConnection.isEnabled = true
            }
        }
    }

    companion object {
        const val TAG: String = "ServiceStatusFragment"
    }
}
