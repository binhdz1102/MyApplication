package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.domain.model.ControlPanelState
import com.example.myapplication.presentation.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint(AppCompatActivity::class)
class MainActivity : Hilt_MainActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupClickListeners()
        observeState()
    }

    private fun applyWindowInsets() {
        val initialLeft = binding.main.paddingLeft
        val initialTop = binding.main.paddingTop
        val initialRight = binding.main.paddingRight
        val initialBottom = binding.main.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = initialLeft + systemBars.left,
                top = initialTop + systemBars.top,
                right = initialRight + systemBars.right,
                bottom = initialBottom + systemBars.bottom,
            )
            insets
        }

        ViewCompat.requestApplyInsets(binding.main)
    }

    private fun setupClickListeners() {
        binding.buttonA11.contentDescription = "Connect to AIDL service"
        binding.buttonA11.setOnClickListener {
            Log.d(TAG, "Area A - connect service clicked")
            viewModel.connect()
        }
        binding.buttonA12.contentDescription = "Disconnect from AIDL service"
        binding.buttonA12.setOnClickListener {
            Log.d(TAG, "Area A - disconnect service clicked")
            viewModel.disconnect()
        }
        bindLogButton(binding.buttonA13, "Area A - row 1 - button 3")
        bindLogButton(binding.buttonB1, "Area B - button 1")
        bindLogButton(binding.buttonB2, "Area B - button 2")
        bindLogButton(binding.buttonB3, "Area B - button 3")
        bindLogButton(binding.buttonB5, "Area B - button 5")
        bindLogButton(binding.buttonB6, "Area B - button 6")
        bindLogButton(binding.buttonB7, "Area B - button 7")

        binding.buttonA21.setOnClickListener {
            Log.d(TAG, "Area A - decrease rating clicked")
            viewModel.decreaseRating()
        }

        binding.buttonA23.setOnClickListener {
            Log.d(TAG, "Area A - increase rating clicked")
            viewModel.increaseRating()
        }

        binding.buttonA31.setOnClickListener {
            Log.d(TAG, "Area A - decrease temperature clicked")
            viewModel.decreaseTemperature()
        }

        binding.buttonA33.setOnClickListener {
            Log.d(TAG, "Area A - increase temperature clicked")
            viewModel.increaseTemperature()
        }

        binding.buttonB4.setOnClickListener {
            Log.d(TAG, "Area B - center toggle clicked")
            viewModel.toggleCenterButton()
        }
    }

    private fun observeState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.controlState.collect(::render)
            }
        }
    }

    private fun render(state: ControlPanelState) {
        renderRating(state.rating)
        binding.valueA32.text = state.temperature.toString()
        binding.valueA32.contentDescription = "Current temperature is ${state.temperature}"
        binding.buttonB4.isSelected = state.toggledOn
        binding.buttonB4.contentDescription = if (state.toggledOn) {
            "Center toggle is on"
        } else {
            "Center toggle is off"
        }

        updateServiceControls(state.isServiceConnected)
    }

    private fun renderRating(rating: Int) {
        val starViews = listOf(
            binding.star1,
            binding.star2,
            binding.star3,
            binding.star4,
            binding.star5,
        )

        starViews.forEachIndexed { index, imageView ->
            val isFilled = index < rating
            imageView.setImageResource(
                if (isFilled) {
                    R.drawable.icon_solid_star
                } else {
                    R.drawable.icon_empty_star
                },
            )
            imageView.contentDescription = "Rating star ${index + 1} ${if (isFilled) "filled" else "empty"}"
        }
    }

    private fun updateServiceControls(isConnected: Boolean) {
        val controlledViews = listOf<View>(
            binding.buttonA21,
            binding.buttonA23,
            binding.buttonA31,
            binding.buttonA33,
            binding.buttonB4,
        )

        controlledViews.forEach { view ->
            view.isEnabled = isConnected
            view.alpha = if (isConnected) 1f else 0.5f
        }
        binding.buttonA11.isEnabled = true
        binding.buttonA11.alpha = if (isConnected) 0.85f else 1f
        binding.buttonA12.isEnabled = isConnected
        binding.buttonA12.alpha = if (isConnected) 1f else 0.5f
        binding.valueA32.alpha = if (isConnected) 1f else 0.7f
    }

    private fun bindLogButton(button: View, description: String) {
        button.contentDescription = description
        button.setOnClickListener {
            Log.d(TAG, "$description clicked")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}
