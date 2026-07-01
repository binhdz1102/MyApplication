package com.example.myapplication.feature.launcher.media

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.myapplication.core.ui.ext.collectLatestState
import com.example.myapplication.feature.launcher.databinding.FragmentMediaCardBinding
import com.example.myapplication.feature.launcher.util.toTimestamp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MediaCardFragment : Fragment() {
    private var bindingReference: FragmentMediaCardBinding? = null
    private val binding get() = checkNotNull(bindingReference)
    private val viewModel: MediaCardViewModel by viewModels()
    private var isTracking = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        bindingReference = FragmentMediaCardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        binding.previousButton.setOnClickListener {
            viewModel.onEvent(MediaCardUiEvent.PreviousTrack)
        }
        binding.playPauseButton.setOnClickListener {
            viewModel.onEvent(MediaCardUiEvent.TogglePlayback)
        }
        binding.nextButton.setOnClickListener {
            viewModel.onEvent(MediaCardUiEvent.NextTrack)
        }
        binding.progressSeekBar.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(
                    seekBar: SeekBar?,
                    progress: Int,
                    fromUser: Boolean,
                ) {
                    if (fromUser) {
                        binding.currentTimeValue.text = progress.toTimestamp()
                    }
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isTracking = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isTracking = false
                    viewModel.onEvent(MediaCardUiEvent.SeekTo(seekBar?.progress ?: 0))
                }
            },
        )

        viewModel.onEvent(MediaCardUiEvent.Observe)
        collectLatestState(viewModel.uiState) { state ->
            binding.songValue.text = state.songName
            binding.appValue.text = state.appName
            binding.totalTimeValue.text = state.durationSeconds.toTimestamp()
            binding.currentTimeValue.text = state.positionSeconds.toTimestamp()
            binding.progressSeekBar.max = state.durationSeconds
            if (!isTracking) {
                binding.progressSeekBar.progress = state.positionSeconds
            }
            binding.playPauseButton.setImageResource(
                if (state.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        bindingReference = null
    }
}
