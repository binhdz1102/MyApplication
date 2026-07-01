package com.example.myapplication.feature.launcher.weather

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.myapplication.core.model.WeatherCondition
import com.example.myapplication.core.ui.ext.collectLatestState
import com.example.myapplication.feature.launcher.R
import com.example.myapplication.feature.launcher.databinding.FragmentWeatherCardBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WeatherCardFragment : Fragment() {
    private var bindingReference: FragmentWeatherCardBinding? = null
    private val binding get() = checkNotNull(bindingReference)
    private val viewModel: WeatherCardViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingReference = FragmentWeatherCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onEvent(WeatherCardUiEvent.Observe)
        collectLatestState(viewModel.uiState) { state ->
            binding.temperatureValue.text = state.temperatureText
            binding.humidityValue.text = state.humidityText
            binding.conditionValue.text =
                when (state.condition) {
                    WeatherCondition.SUNNY -> getString(R.string.weather_sunny)
                    WeatherCondition.SUN_WITH_CLOUD -> getString(R.string.weather_sun_cloud)
                    WeatherCondition.CLOUDY -> getString(R.string.weather_cloudy)
                    WeatherCondition.RAINY -> getString(R.string.weather_rainy)
                    WeatherCondition.SNOWY -> getString(R.string.weather_snowy)
                    WeatherCondition.THUNDER -> getString(R.string.weather_thunder)
                }
            binding.locationValue.text = state.locationLabel
            binding.weatherIcon.setImageResource(
                when (state.condition) {
                    WeatherCondition.SUNNY -> R.drawable.sunny
                    WeatherCondition.SUN_WITH_CLOUD -> R.drawable.sun_with_cloud
                    WeatherCondition.CLOUDY -> R.drawable.cloudy
                    WeatherCondition.RAINY -> R.drawable.rainy
                    WeatherCondition.SNOWY -> R.drawable.snowy
                    WeatherCondition.THUNDER -> R.drawable.thunder
                },
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingReference = null
    }
}
