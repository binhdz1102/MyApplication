package com.example.myapplication.feature.launcher.navigation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.myapplication.core.ui.ext.collectLatestState
import com.example.myapplication.feature.launcher.R
import com.example.myapplication.feature.launcher.databinding.FragmentNavigationPreviewBinding
import com.example.myapplication.feature.launcher.util.VerticalSwipeTouchListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NavigationPreviewFragment : Fragment() {
    private var bindingReference: FragmentNavigationPreviewBinding? = null
    private val binding get() = checkNotNull(bindingReference)
    private val viewModel: NavigationHostViewModel by viewModels(ownerProducer = { requireParentFragment() })

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingReference = FragmentNavigationPreviewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        Glide
            .with(this)
            .asGif()
            .load(R.drawable.navigation_screen)
            .into(binding.navigationImage)

        binding.swipeHandle.setOnTouchListener(
            VerticalSwipeTouchListener(
                onSwipeUp = {
                    viewModel.onEvent(NavigationHostUiEvent.ShowAppList)
                },
            ),
        )

        collectLatestState(viewModel.uiState) { state ->
            val card = state.navigationCard ?: return@collectLatestState
            binding.destinationValue.text = card.destination
            binding.etaValue.text = card.etaLabel
            binding.distanceValue.text = card.distanceLabel
            binding.guidanceValue.text = card.guidanceLabel
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingReference = null
    }
}
