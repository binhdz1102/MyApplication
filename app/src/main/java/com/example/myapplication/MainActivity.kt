package com.example.myapplication

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.example.myapplication.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var currentX = 10
    private var currentRating = 5

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyWindowInsets()
        setupButtons()
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

    private fun setupButtons() {
        bindLogButton(binding.buttonA11, "Area A - row 1 - button 1")
        bindLogButton(binding.buttonA12, "Area A - row 1 - button 2")
        bindLogButton(binding.buttonA13, "Area A - row 1 - button 3")
        setupRatingControls()
        bindDecreaseButton()
        updateXValue()
        bindIncreaseButton()
        bindLogButton(binding.buttonB1, "Area B - button 1")
        bindLogButton(binding.buttonB2, "Area B - button 2")
        bindLogButton(binding.buttonB3, "Area B - button 3")
        bindSelectableCenterButton(binding.buttonB4)
        bindLogButton(binding.buttonB5, "Area B - button 5")
        bindLogButton(binding.buttonB6, "Area B - button 6")
        bindLogButton(binding.buttonB7, "Area B - button 7")
    }

    private fun bindLogButton(button: View, description: String) {
        button.contentDescription = description
        button.setOnClickListener {
            Log.d(TAG, "$description clicked")
        }
    }

    private fun setupRatingControls() {
        binding.buttonA21.contentDescription = "Area A - decrease rating"
        binding.buttonA21.setOnClickListener {
            if (currentRating > 0) {
                currentRating -= 1
                updateRatingStars()
            }
            Log.d(TAG, "Area A - decrease rating clicked, rating=$currentRating")
        }

        binding.buttonA23.contentDescription = "Area A - increase rating"
        binding.buttonA23.setOnClickListener {
            if (currentRating < MAX_RATING) {
                currentRating += 1
                updateRatingStars()
            }
            Log.d(TAG, "Area A - increase rating clicked, rating=$currentRating")
        }

        updateRatingStars()
    }

    private fun bindDecreaseButton() {
        binding.buttonA31.contentDescription = "Area A - decrease X"
        binding.buttonA31.setOnClickListener {
            currentX -= 1
            updateXValue()
            Log.d(TAG, "Area A - decrease X clicked, X=$currentX")
        }
    }

    private fun bindIncreaseButton() {
        binding.buttonA33.contentDescription = "Area A - increase X"
        binding.buttonA33.setOnClickListener {
            currentX += 1
            updateXValue()
            Log.d(TAG, "Area A - increase X clicked, X=$currentX")
        }
    }

    private fun updateXValue() {
        binding.valueA32.text = currentX.toString()
        binding.valueA32.contentDescription = "Area A - X value $currentX"
    }

    private fun updateRatingStars() {
        val starViews = listOf(
            binding.star1,
            binding.star2,
            binding.star3,
            binding.star4,
            binding.star5,
        )

        starViews.forEachIndexed { index, imageView ->
            val iconRes = if (index < currentRating) {
                R.drawable.icon_solid_star
            } else {
                R.drawable.icon_empty_star
            }
            imageView.setImageResource(iconRes)
            imageView.contentDescription = "Area A - star ${index + 1} ${if (index < currentRating) "filled" else "empty"}"
        }
    }

    private fun bindSelectableCenterButton(button: View) {
        button.contentDescription = "Area B - center button"
        button.isSelected = true
        button.setOnClickListener {
            it.isSelected = !it.isSelected
            Log.d(TAG, "Area B - center button clicked, selected=${it.isSelected}")
        }
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val MAX_RATING = 5
    }
}
