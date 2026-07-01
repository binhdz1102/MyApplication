package com.example.myapplication.feature.launcher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.myapplication.core.ui.ext.playEntrance
import com.example.myapplication.feature.launcher.databinding.FragmentLauncherDashboardBinding
import com.example.myapplication.feature.launcher.date.DateCardFragment
import com.example.myapplication.feature.launcher.media.MediaCardFragment
import com.example.myapplication.feature.launcher.navigation.NavigationHostFragment
import com.example.myapplication.feature.launcher.weather.WeatherCardFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LauncherDashboardFragment : Fragment() {
    private var bindingReference: FragmentLauncherDashboardBinding? = null
    private val binding get() = checkNotNull(bindingReference)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingReference = FragmentLauncherDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                replace(binding.navigationContainer.id, NavigationHostFragment())
                replace(binding.dateContainer.id, DateCardFragment())
                replace(binding.weatherContainer.id, WeatherCardFragment())
                replace(binding.mediaContainer.id, MediaCardFragment())
            }
        }

        listOf(
            binding.navigationContainer,
            binding.dateContainer,
            binding.weatherContainer,
            binding.mediaContainer,
        ).forEachIndexed { index, fragmentContainerView ->
            fragmentContainerView.playEntrance(delayMs = index * 80L)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingReference = null
    }
}
