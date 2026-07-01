package com.example.myapplication.feature.launcher.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.core.ui.ext.collectLatestState
import com.example.myapplication.feature.launcher.R
import com.example.myapplication.feature.launcher.databinding.FragmentAppListBinding
import com.example.myapplication.feature.launcher.navigation.NavigationHostUiEvent
import com.example.myapplication.feature.launcher.navigation.NavigationHostViewModel
import com.example.myapplication.feature.launcher.util.VerticalSwipeTouchListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AppListFragment : Fragment() {
    private var bindingReference: FragmentAppListBinding? = null
    private val binding get() = checkNotNull(bindingReference)
    private val viewModel: NavigationHostViewModel by viewModels(ownerProducer = { requireParentFragment() })
    private lateinit var appPagesAdapter: AppPagesAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingReference = FragmentAppListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        appPagesAdapter =
            AppPagesAdapter { shortcut ->
                Toast.makeText(requireContext(), "${shortcut.label} is mocked", Toast.LENGTH_SHORT).show()
            }

        binding.appsPager.adapter = appPagesAdapter
        binding.appsPager.registerOnPageChangeCallback(
            object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    viewModel.onEvent(NavigationHostUiEvent.UpdateAppPage(position))
                }
            },
        )

        binding.collapseHandle.setOnTouchListener(
            VerticalSwipeTouchListener(
                onSwipeDown = {
                    viewModel.onEvent(NavigationHostUiEvent.ShowNavigation)
                },
            ),
        )

        collectLatestState(viewModel.uiState) { state ->
            appPagesAdapter.submitPages(state.appPages)
            binding.pageIndicator.text =
                getString(
                    R.string.page_indicator,
                    state.currentAppPage + 1,
                    state.appPages.size.coerceAtLeast(1),
                )
            if (binding.appsPager.currentItem != state.currentAppPage && state.appPages.isNotEmpty()) {
                binding.appsPager.setCurrentItem(state.currentAppPage, false)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingReference = null
    }
}
