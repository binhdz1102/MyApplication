package com.example.myapplication.presentation.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentAidlUserListBinding
import com.example.myapplication.di.AppContainer
import com.example.myapplication.domain.model.User
import com.example.myapplication.presentation.model.ServiceConnectionUiState
import com.example.myapplication.presentation.ui.adapter.UserAdapter
import com.example.myapplication.presentation.ui.dialog.DeleteUserDialogFragment
import com.example.myapplication.presentation.ui.dialog.UserFormDialogFragment
import com.example.myapplication.presentation.viewmodel.UserViewModel
import com.google.android.material.snackbar.Snackbar

class AidlUserListFragment : Fragment(), UserActionHandler {

    private enum class SourceMode {
        AIDL,
        AIDL_ROOM,
    }

    private var _binding: FragmentAidlUserListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: UserViewModel
    private var screenConfigurationHost: ScreenConfigurationHost? = null
    private var currentConnectionState: ServiceConnectionUiState = ServiceConnectionUiState.DISCONNECTED
    private lateinit var sourceMode: SourceMode

    private val userAdapter by lazy(LazyThreadSafetyMode.NONE) {
        UserAdapter(
            onItemClick = { user -> showEditUserDialog(user) },
            onItemLongClick = { user -> showDeleteUserDialog(user) },
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        screenConfigurationHost = context as? ScreenConfigurationHost
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sourceMode = when (arguments?.getString(ARG_SOURCE_MODE)) {
            SOURCE_MODE_AIDL_ROOM -> SourceMode.AIDL_ROOM
            else -> SourceMode.AIDL
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentAidlUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            if (sourceMode == SourceMode.AIDL_ROOM) {
                AppContainer.provideAidlRoomUserViewModelFactory(requireContext().applicationContext)
            } else {
                AppContainer.provideAidlUserViewModelFactory(requireContext().applicationContext)
            },
        )[UserViewModel::class.java]

        binding.recyclerUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        binding.buttonRetryConnection.setOnClickListener {
            viewModel.connectService()
        }

        registerDialogResults()
        observeViewModel()
        viewModel.connectService()
    }

    override fun onResume() {
        super.onResume()
        updateScreenConfiguration()
    }

    override fun onAddRequested() {
        if (currentConnectionState != ServiceConnectionUiState.CONNECTED) {
            Snackbar.make(binding.root, R.string.message_service_not_connected, Snackbar.LENGTH_SHORT).show()
            return
        }
        UserFormDialogFragment.newAddDialog()
            .show(childFragmentManager, UserFormDialogFragment.TAG)
    }

    override fun onRefreshRequested() {
        if (currentConnectionState == ServiceConnectionUiState.CONNECTED) {
            viewModel.loadUsers()
        } else {
            viewModel.connectService()
        }
    }

    override fun onDestroyView() {
        binding.recyclerUsers.adapter = null
        _binding = null
        super.onDestroyView()
    }

    override fun onDetach() {
        screenConfigurationHost = null
        super.onDetach()
    }

    private fun registerDialogResults() {
        childFragmentManager.setFragmentResultListener(
            UserFormDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            val user = User(
                id = bundle.getLong(UserFormDialogFragment.RESULT_USER_ID),
                name = bundle.getString(UserFormDialogFragment.RESULT_USER_NAME).orEmpty(),
                age = bundle.getInt(UserFormDialogFragment.RESULT_USER_AGE),
                weight = bundle.getFloat(UserFormDialogFragment.RESULT_USER_WEIGHT),
            )

            if (bundle.getBoolean(UserFormDialogFragment.RESULT_IS_EDIT_MODE)) {
                viewModel.updateUser(user)
            } else {
                viewModel.addUser(user)
            }
        }

        childFragmentManager.setFragmentResultListener(
            DeleteUserDialogFragment.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, bundle ->
            viewModel.deleteUser(bundle.getLong(DeleteUserDialogFragment.RESULT_USER_ID))
        }
    }

    private fun observeViewModel() {
        viewModel.users.observe(viewLifecycleOwner) { users ->
            userAdapter.submitList(users)
            binding.layoutEmpty.isVisible =
                currentConnectionState == ServiceConnectionUiState.CONNECTED &&
                    users.isEmpty() &&
                    viewModel.isLoading.value != true
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressLoadUsers.isVisible =
                currentConnectionState == ServiceConnectionUiState.CONNECTED && isLoading
            binding.textLoading.isVisible =
                currentConnectionState == ServiceConnectionUiState.CONNECTED && isLoading
            binding.layoutEmpty.isVisible =
                currentConnectionState == ServiceConnectionUiState.CONNECTED &&
                    !isLoading &&
                    userAdapter.currentList.isEmpty()
        }

        viewModel.serviceConnectionState.observe(viewLifecycleOwner) { state ->
            currentConnectionState = state
            renderConnectionState(state)
            updateScreenConfiguration()
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }
    }

    private fun renderConnectionState(state: ServiceConnectionUiState) {
        val isConnected = state == ServiceConnectionUiState.CONNECTED
        binding.layoutServiceStatus.isVisible = !isConnected
        binding.layoutListContent.isVisible = isConnected

        when (state) {
            ServiceConnectionUiState.CONNECTING -> {
                binding.textStatusTitle.setText(R.string.service_connecting_title)
                binding.textStatusMessage.setText(R.string.service_connecting_message)
                binding.buttonRetryConnection.setText(R.string.action_connecting)
                binding.buttonRetryConnection.isEnabled = false
            }

            ServiceConnectionUiState.CONNECTED -> {
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

    private fun updateScreenConfiguration() {
        screenConfigurationHost?.updateScreenConfiguration(
            ScreenConfiguration(
                titleRes = if (sourceMode == SourceMode.AIDL_ROOM) {
                    R.string.title_aidl_room_users
                } else {
                    R.string.title_aidl_users
                },
                showBackButton = true,
                showFab = currentConnectionState == ServiceConnectionUiState.CONNECTED,
                showRefresh = true,
            ),
        )
    }

    private fun showEditUserDialog(user: User) {
        UserFormDialogFragment.newEditDialog(user)
            .show(childFragmentManager, UserFormDialogFragment.TAG)
    }

    private fun showDeleteUserDialog(user: User) {
        DeleteUserDialogFragment.newInstance(user.id, user.name)
            .show(childFragmentManager, DeleteUserDialogFragment.TAG)
    }

    companion object {
        const val TAG_AIDL: String = "AidlUserListFragment"
        const val TAG_AIDL_ROOM: String = "AidlRoomUserListFragment"

        private const val ARG_SOURCE_MODE: String = "arg_source_mode"
        private const val SOURCE_MODE_AIDL: String = "source_mode_aidl"
        private const val SOURCE_MODE_AIDL_ROOM: String = "source_mode_aidl_room"

        fun newAidlFragment(): AidlUserListFragment {
            return AidlUserListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SOURCE_MODE, SOURCE_MODE_AIDL)
                }
            }
        }

        fun newAidlRoomDatabaseFragment(): AidlUserListFragment {
            return AidlUserListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_SOURCE_MODE, SOURCE_MODE_AIDL_ROOM)
                }
            }
        }
    }
}
