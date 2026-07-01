package com.example.myapplication.feature.launcher.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.myapplication.core.ui.ext.collectLatestState
import com.example.myapplication.feature.launcher.apps.AppListFragment
import com.example.myapplication.feature.launcher.databinding.FragmentNavigationHostBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.abs

@AndroidEntryPoint
class NavigationHostFragment : Fragment() {
    private var bindingReference: FragmentNavigationHostBinding? = null
    private val binding get() = checkNotNull(bindingReference)
    private val viewModel: NavigationHostViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingReference = FragmentNavigationHostBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.surfacePager.adapter = NavigationSurfacePagerAdapter(this)
        binding.surfacePager.orientation = ViewPager2.ORIENTATION_VERTICAL
        binding.surfacePager.isUserInputEnabled = false
        binding.surfacePager.setPageTransformer { page, position ->
            page.alpha = 0.72f + ((1f - abs(position)) * 0.28f)
            page.translationY = position * page.height * 0.08f
        }

        viewModel.onEvent(NavigationHostUiEvent.Initialize)

        collectLatestState(viewModel.uiState) { state ->
            if (binding.surfacePager.currentItem != state.activeSurface.pageIndex) {
                binding.surfacePager.setCurrentItem(state.activeSurface.pageIndex, true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingReference = null
    }

    private class NavigationSurfacePagerAdapter(
        fragment: Fragment,
    ) : FragmentStateAdapter(fragment) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment =
            when (position) {
                0 -> NavigationPreviewFragment()
                else -> AppListFragment()
            }
    }
}
