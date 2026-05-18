package com.example.myapplication.presentation.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSourcePickerBinding

class SourcePickerFragment : Fragment() {

    interface Navigator {
        fun openAidlUsers()
        fun openRoomUsers()
        fun openAidlRoomUsers()
    }

    private var _binding: FragmentSourcePickerBinding? = null
    private val binding get() = _binding!!

    private var navigator: Navigator? = null
    private var screenConfigurationHost: ScreenConfigurationHost? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        navigator = context as? Navigator
        screenConfigurationHost = context as? ScreenConfigurationHost
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentSourcePickerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonUseAidl.setOnClickListener {
            navigator?.openAidlUsers()
        }
        binding.buttonUseRoom.setOnClickListener {
            navigator?.openRoomUsers()
        }
        binding.buttonUseAidlRoom.setOnClickListener {
            navigator?.openAidlRoomUsers()
        }
    }

    override fun onResume() {
        super.onResume()
        screenConfigurationHost?.updateScreenConfiguration(
            ScreenConfiguration(
                titleRes = R.string.title_source_picker,
                showBackButton = false,
                showFab = false,
                showRefresh = false,
            ),
        )
    }

    override fun onDetach() {
        navigator = null
        screenConfigurationHost = null
        super.onDetach()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG: String = "SourcePickerFragment"
    }
}
