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
import com.example.myapplication.databinding.FragmentUserListBinding
import com.example.myapplication.di.AppContainer
import com.example.myapplication.domain.model.User
import com.example.myapplication.presentation.ui.adapter.UserAdapter
import com.example.myapplication.presentation.viewmodel.UserViewModel

class UserListFragment : Fragment() {

    interface UserItemListener {
        fun onUserClicked(user: User)
        fun onUserLongClicked(user: User)
    }

    private var _binding: FragmentUserListBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: UserViewModel
    private var userItemListener: UserItemListener? = null

    private val userAdapter by lazy(LazyThreadSafetyMode.NONE) {
        UserAdapter(
            onItemClick = { user -> userItemListener?.onUserClicked(user) },
            onItemLongClick = { user -> userItemListener?.onUserLongClicked(user) },
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        userItemListener = context as? UserItemListener
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
            requireActivity(),
            AppContainer.provideUserViewModelFactory(requireContext().applicationContext),
        )[UserViewModel::class.java]

        binding.recyclerUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }

        observeViewModel()
    }

    override fun onDetach() {
        userItemListener = null
        super.onDetach()
    }

    override fun onDestroyView() {
        binding.recyclerUsers.adapter = null
        _binding = null
        super.onDestroyView()
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
    }

    companion object {
        const val TAG: String = "UserListFragment"
    }
}
