package com.example.myapplication.presentation.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentSourcePickerBinding;
import com.example.myapplication.presentation.model.FeatureOption;
import com.example.myapplication.presentation.ui.adapter.FeatureOptionAdapter;

import java.util.ArrayList;
import java.util.List;

public class SourcePickerFragment extends Fragment {

    public static final String TAG = "SourcePickerFragment";

    public interface Navigator {
        void openAidlUsers();

        void openRoomUsers();

        void openAidlRoomUsers();

        void openBasicMusicPlayer();

        void openMediaSessionPreview();

        void openBackgroundPlayback();
    }

    private FragmentSourcePickerBinding binding;
    private Navigator navigator;
    private ScreenConfigurationHost screenConfigurationHost;
    private final FeatureOptionAdapter featureOptionAdapter =
            new FeatureOptionAdapter(this::handleFeatureSelected);

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
        binding.recyclerFeatures.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerFeatures.setAdapter(featureOptionAdapter);
        featureOptionAdapter.submitList(buildFeatureOptions());
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
        if (binding != null) {
            binding.recyclerFeatures.setAdapter(null);
        }
        binding = null;
        super.onDestroyView();
    }

    private List<FeatureOption> buildFeatureOptions() {
        List<FeatureOption> options = new ArrayList<>();
        options.add(new FeatureOption(
                FeatureOption.Destination.AIDL_USERS,
                R.string.source_option_aidl_title,
                R.string.source_option_aidl_message
        ));
        options.add(new FeatureOption(
                FeatureOption.Destination.ROOM_USERS,
                R.string.source_option_room_title,
                R.string.source_option_room_message
        ));
        options.add(new FeatureOption(
                FeatureOption.Destination.AIDL_ROOM_USERS,
                R.string.source_option_aidl_room_title,
                R.string.source_option_aidl_room_message
        ));
        options.add(new FeatureOption(
                FeatureOption.Destination.MUSIC_PLAYER,
                R.string.feature_option_music_title,
                R.string.feature_option_music_message
        ));
        options.add(new FeatureOption(
                FeatureOption.Destination.MEDIA_SESSION_PREVIEW,
                R.string.feature_option_media_session_title,
                R.string.feature_option_media_session_message
        ));
        options.add(new FeatureOption(
                FeatureOption.Destination.BACKGROUND_PLAYBACK,
                R.string.feature_option_background_playback_title,
                R.string.feature_option_background_playback_message
        ));
        return options;
    }

    private void handleFeatureSelected(FeatureOption option) {
        if (navigator == null || option == null) {
            return;
        }

        FeatureOption.Destination destination = option.getDestination();
        if (destination == FeatureOption.Destination.AIDL_USERS) {
            navigator.openAidlUsers();
        } else if (destination == FeatureOption.Destination.ROOM_USERS) {
            navigator.openRoomUsers();
        } else if (destination == FeatureOption.Destination.AIDL_ROOM_USERS) {
            navigator.openAidlRoomUsers();
        } else if (destination == FeatureOption.Destination.MUSIC_PLAYER) {
            navigator.openBasicMusicPlayer();
        } else if (destination == FeatureOption.Destination.MEDIA_SESSION_PREVIEW) {
            navigator.openMediaSessionPreview();
        } else if (destination == FeatureOption.Destination.BACKGROUND_PLAYBACK) {
            navigator.openBackgroundPlayback();
        }
    }
}
