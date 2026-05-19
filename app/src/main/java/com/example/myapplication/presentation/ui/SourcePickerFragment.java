package com.example.myapplication.presentation.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentSourcePickerBinding;

public class SourcePickerFragment extends Fragment {

    public static final String TAG = "SourcePickerFragment";

    public interface Navigator {
        void openAidlUsers();

        void openRoomUsers();

        void openAidlRoomUsers();
    }

    private FragmentSourcePickerBinding binding;
    private Navigator navigator;
    private ScreenConfigurationHost screenConfigurationHost;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof Navigator) {
            navigator = (Navigator) context;
        }
        if (context instanceof ScreenConfigurationHost) {
            screenConfigurationHost = (ScreenConfigurationHost) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentSourcePickerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.buttonUseAidl.setOnClickListener(v -> {
            if (navigator != null) {
                navigator.openAidlUsers();
            }
        });
        binding.buttonUseRoom.setOnClickListener(v -> {
            if (navigator != null) {
                navigator.openRoomUsers();
            }
        });
        binding.buttonUseAidlRoom.setOnClickListener(v -> {
            if (navigator != null) {
                navigator.openAidlRoomUsers();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        if (screenConfigurationHost != null) {
            screenConfigurationHost.updateScreenConfiguration(
                    new ScreenConfiguration(
                            R.string.title_source_picker,
                            false,
                            false,
                            false
                    )
            );
        }
    }

    @Override
    public void onDetach() {
        navigator = null;
        screenConfigurationHost = null;
        super.onDetach();
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }
}
