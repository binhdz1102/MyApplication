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
import com.example.myapplication.databinding.FragmentUserListBinding
import com.example.myapplication.di.AppContainer
import com.example.myapplication.domain.model.User
import com.example.myapplication.presentation.ui.adapter.UserAdapter
import com.example.myapplication.presentation.ui.dialog.DeleteUserDialogFragment
import com.example.myapplication.presentation.ui.dialog.UserFormDialogFragment
import com.example.myapplication.presentation.viewmodel.UserViewModel
import com.google.android.material.snackbar.Snackbar

class RoomUserListFragment : Fragment(), UserActionHandler {

    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: UserViewModel
    private var screenConfigurationHost: ScreenConfigurationHost? = null

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentUserListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(
            this,
            AppContainer.provideRoomUserViewModelFactory(requireContext().applicationContext),
        )[UserViewModel::class.java]

        binding.recyclerUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
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
        UserFormDialogFragment.newAddDialog()
            .show(childFragmentManager, UserFormDialogFragment.TAG)
    }

    override fun onRefreshRequested() {
        viewModel.loadUsers()
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
            binding.layoutEmpty.isVisible = users.isEmpty() && viewModel.isLoading.value != true
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressLoadUsers.isVisible = isLoading
            binding.textLoading.isVisible = isLoading
            binding.layoutEmpty.isVisible = !isLoading && userAdapter.currentList.isEmpty()
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (!message.isNullOrBlank()) {
                Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                viewModel.consumeMessage()
            }
        }
    }

    private fun updateScreenConfiguration() {
        screenConfigurationHost?.updateScreenConfiguration(
            ScreenConfiguration(
                titleRes = R.string.title_room_users,
                showBackButton = true,
                showFab = true,
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
        const val TAG: String = "RoomUserListFragment"
    }
}
