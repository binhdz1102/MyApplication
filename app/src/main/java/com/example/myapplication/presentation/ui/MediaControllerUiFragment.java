package com.example.myapplication.presentation.ui;

import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentMediaControllerUiBinding;
import com.example.myapplication.playback.PlaybackService;
import com.example.myapplication.presentation.media.SongAssetLoader;
import com.example.myapplication.presentation.model.SongItem;
import com.example.myapplication.presentation.ui.adapter.SongAdapter;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MediaControllerUiFragment extends Fragment {

    public static final String TAG = "MediaControllerUiFragment";
    private static final long PROGRESS_UPDATE_INTERVAL_MS = 500L;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final List<SongItem> songs = new ArrayList<>();
    private final SongAdapter songAdapter = new SongAdapter(this::playSongAt);

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgressUi();
            updateControllerUi();
            if (binding != null) {
                progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS);
            }
        }
    };

    private final Player.Listener controllerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            updateSongSelection();
            updateControllerUi();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            updateSongSelection();
            updateControllerUi();
        }

        @Override
        public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
            updateControllerUi();
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            updateSongSelection();
            updateControllerUi();
        }
    };

    private FragmentMediaControllerUiBinding binding;
    private ScreenConfigurationHost screenConfigurationHost;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController mediaController;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
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
        binding = FragmentMediaControllerUiBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        binding.recyclerSongs.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerSongs.setAdapter(songAdapter);
        binding.seekPlayback.setOnSeekBarChangeListener(createSeekBarListener());
        binding.buttonPlay.setOnClickListener(v -> playCurrentSelection());
        binding.buttonPause.setOnClickListener(v -> pausePlayback());
        binding.buttonPrevious.setOnClickListener(v -> playPrevious());
        binding.buttonNext.setOnClickListener(v -> playNext());

        songs.clear();
        songs.addAll(SongAssetLoader.loadSongs(requireContext()));
        songAdapter.submitList(songs);
        binding.textEmptySongs.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerSongs.setVisibility(songs.isEmpty() ? View.GONE : View.VISIBLE);

        connectToPlaybackService();
        updateControllerUi();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (screenConfigurationHost != null) {
            screenConfigurationHost.updateScreenConfiguration(
                    new ScreenConfiguration(
                            R.string.title_media_controller_ui,
                            true,
                            false,
                            false
                    )
            );
        }
    }

    @Override
    public void onDestroyView() {
        releaseController();
        if (binding != null) {
            binding.recyclerSongs.setAdapter(null);
            binding = null;
        }
        super.onDestroyView();
    }

    @Override
    public void onDetach() {
        screenConfigurationHost = null;
        super.onDetach();
    }

    private void connectToPlaybackService() {
        SessionToken sessionToken = new SessionToken(
                requireContext(),
                new ComponentName(requireContext(), PlaybackService.class)
        );
        controllerFuture = new MediaController.Builder(requireContext(), sessionToken)
                .buildAsync();
        controllerFuture.addListener(this::attachControllerWhenReady, ContextCompat.getMainExecutor(requireContext()));
    }

    private void attachControllerWhenReady() {
        if (!isAdded() || controllerFuture == null) {
            return;
        }

        try {
            mediaController = controllerFuture.get();
            mediaController.addListener(controllerListener);
            ensurePlaylistInitialized();
            progressHandler.post(progressRunnable);
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }

        updateSongSelection();
        updateControllerUi();
    }

    private void ensurePlaylistInitialized() {
        if (mediaController == null || songs.isEmpty() || mediaController.getMediaItemCount() > 0) {
            return;
        }

        List<MediaItem> mediaItems = new ArrayList<>();
        for (SongItem song : songs) {
            mediaItems.add(song.toMediaItem());
        }

        mediaController.setMediaItems(mediaItems, 0, 0L);
        mediaController.prepare();
    }

    private void playSongAt(int position) {
        if (!isControllerReady() || position == RecyclerView.NO_POSITION || position < 0 || position >= songs.size()) {
            return;
        }

        ensurePlaylistInitialized();
        mediaController.seekToDefaultPosition(position);
        if (mediaController.getPlaybackState() == Player.STATE_IDLE) {
            mediaController.prepare();
        }
        mediaController.play();
        updateSongSelection();
        updateControllerUi();
    }

    private void playCurrentSelection() {
        if (!isControllerReady() || songs.isEmpty()) {
            return;
        }

        ensurePlaylistInitialized();
        int currentSongIndex = getCurrentSongIndex();
        if (currentSongIndex == C.INDEX_UNSET || currentSongIndex == RecyclerView.NO_POSITION) {
            mediaController.seekToDefaultPosition(0);
        } else if (mediaController.getPlaybackState() == Player.STATE_ENDED) {
            mediaController.seekTo(currentSongIndex, 0L);
        }

        if (mediaController.getPlaybackState() == Player.STATE_IDLE) {
            mediaController.prepare();
        }

        mediaController.play();
        updateSongSelection();
        updateControllerUi();
    }

    private void pausePlayback() {
        if (!isControllerReady()) {
            return;
        }

        mediaController.pause();
        updateSongSelection();
        updateControllerUi();
    }

    private void playPrevious() {
        if (!isControllerReady() || songs.isEmpty()) {
            return;
        }

        ensurePlaylistInitialized();
        int currentSongIndex = getCurrentSongIndex();
        if (currentSongIndex == C.INDEX_UNSET || currentSongIndex == RecyclerView.NO_POSITION) {
            playSongAt(0);
            return;
        }

        playSongAt(Math.max(currentSongIndex - 1, 0));
    }

    private void playNext() {
        if (!isControllerReady() || songs.isEmpty()) {
            return;
        }

        ensurePlaylistInitialized();
        int currentSongIndex = getCurrentSongIndex();
        if (currentSongIndex == C.INDEX_UNSET || currentSongIndex == RecyclerView.NO_POSITION) {
            playSongAt(0);
            return;
        }

        playSongAt(Math.min(currentSongIndex + 1, songs.size() - 1));
    }

    private void updateControllerUi() {
        if (binding == null) {
            return;
        }

        boolean isConnected = isControllerReady();
        SongItem currentSong = getCurrentSong();
        int currentSongIndex = getCurrentSongIndex();
        long currentDuration = resolveCurrentSongDuration(currentSong);
        MediaMetadata observedMetadata = isConnected ? mediaController.getMediaMetadata() : MediaMetadata.EMPTY;

        binding.textControllerStatusValue.setText(
                isConnected ? R.string.status_connected : R.string.status_disconnected
        );
        binding.textPlaybackStateValue.setText(getPlaybackStateLabel());
        binding.textPlayWhenReadyValue.setText(
                isConnected && mediaController.getPlayWhenReady()
                        ? R.string.status_true
                        : R.string.status_false
        );
        binding.textIsPlayingValue.setText(
                isConnected && mediaController.isPlaying()
                        ? R.string.status_true
                        : R.string.status_false
        );

        binding.textObservedTitleValue.setText(
                observedMetadata.title == null || observedMetadata.title.length() == 0
                        ? getString(R.string.label_not_available)
                        : observedMetadata.title
        );
        binding.textObservedArtistValue.setText(
                observedMetadata.artist == null || observedMetadata.artist.length() == 0
                        ? getString(R.string.label_unknown_artist)
                        : observedMetadata.artist
        );

        binding.textNowPlayingTitle.setText(
                currentSong == null ? getString(R.string.label_not_available) : currentSong.getTitle()
        );
        binding.textNowPlayingArtist.setText(
                currentSong == null ? getString(R.string.label_unknown_artist) : currentSong.getArtist()
        );

        binding.buttonPlay.setEnabled(isConnected && !songs.isEmpty());
        binding.buttonPause.setEnabled(isConnected && mediaController.isPlaying());
        binding.buttonPrevious.setEnabled(isConnected && currentSongIndex > 0);
        binding.buttonNext.setEnabled(isConnected && currentSongIndex >= 0 && currentSongIndex < songs.size() - 1);
        binding.seekPlayback.setEnabled(isConnected && currentSong != null);

        binding.textDurationValue.setText(formatDuration(currentDuration));
        updateProgressUi();
    }

    private void updateSongSelection() {
        songAdapter.updatePlaybackState(
                getCurrentSongIndex(),
                isControllerReady() && mediaController.isPlaying()
        );
    }

    private void updateProgressUi() {
        if (binding == null) {
            return;
        }

        SongItem currentSong = getCurrentSong();
        long durationMs = resolveCurrentSongDuration(currentSong);
        long positionMs = isControllerReady() ? Math.max(mediaController.getCurrentPosition(), 0L) : 0L;
        long maxProgress = Math.max(durationMs, positionMs);

        if (maxProgress <= 0L) {
            binding.seekPlayback.setMax(1);
            binding.seekPlayback.setProgress(0);
        } else {
            binding.seekPlayback.setMax((int) Math.min(maxProgress, Integer.MAX_VALUE));
            if (!binding.seekPlayback.isPressed()) {
                binding.seekPlayback.setProgress((int) Math.min(positionMs, Integer.MAX_VALUE));
            }
        }

        binding.textCurrentPositionValue.setText(formatDuration(positionMs));
        binding.textDurationValue.setText(formatDuration(durationMs));
    }

    private int getCurrentSongIndex() {
        if (!isControllerReady() || songs.isEmpty()) {
            return RecyclerView.NO_POSITION;
        }

        int currentIndex = mediaController.getCurrentMediaItemIndex();
        if (currentIndex == C.INDEX_UNSET || currentIndex < 0 || currentIndex >= songs.size()) {
            return RecyclerView.NO_POSITION;
        }
        return currentIndex;
    }

    @Nullable
    private SongItem getCurrentSong() {
        if (!isControllerReady() || songs.isEmpty()) {
            return null;
        }

        int currentSongIndex = getCurrentSongIndex();
        if (currentSongIndex == RecyclerView.NO_POSITION) {
            return mediaController.getMediaItemCount() > 0 ? songs.get(0) : null;
        }
        return songs.get(currentSongIndex);
    }

    private long resolveCurrentSongDuration(@Nullable SongItem currentSong) {
        long durationMs = !isControllerReady() ? C.TIME_UNSET : mediaController.getDuration();
        if (durationMs == C.TIME_UNSET || durationMs < 0L) {
            return currentSong == null ? 0L : Math.max(currentSong.getDurationMs(), 0L);
        }
        return durationMs;
    }

    private SeekBar.OnSeekBarChangeListener createSeekBarListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && binding != null) {
                    binding.textCurrentPositionValue.setText(formatDuration(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!isControllerReady() || songs.isEmpty()) {
                    return;
                }

                ensurePlaylistInitialized();
                if (mediaController.getPlaybackState() == Player.STATE_IDLE) {
                    mediaController.prepare();
                }
                mediaController.seekTo(seekBar.getProgress());
                updateProgressUi();
                updateControllerUi();
            }
        };
    }

    private CharSequence getPlaybackStateLabel() {
        if (!isControllerReady()) {
            return getString(R.string.playback_state_unknown);
        }

        int playbackState = mediaController.getPlaybackState();
        if (playbackState == Player.STATE_IDLE) {
            return getString(R.string.playback_state_idle);
        }
        if (playbackState == Player.STATE_BUFFERING) {
            return getString(R.string.playback_state_buffering);
        }
        if (playbackState == Player.STATE_READY) {
            return getString(R.string.playback_state_ready);
        }
        if (playbackState == Player.STATE_ENDED) {
            return getString(R.string.playback_state_ended);
        }
        return getString(R.string.playback_state_unknown);
    }

    private String formatDuration(long durationMs) {
        if (durationMs <= 0L) {
            return "00:00";
        }

        long totalSeconds = durationMs / 1000L;
        long minutes = totalSeconds / 60L;
        long seconds = totalSeconds % 60L;
        return String.format(Locale.US, "%02d:%02d", minutes, seconds);
    }

    private boolean isControllerReady() {
        return mediaController != null && mediaController.isConnected();
    }

    private void releaseController() {
        progressHandler.removeCallbacks(progressRunnable);

        if (mediaController != null) {
            mediaController.removeListener(controllerListener);
            mediaController.release();
            mediaController = null;
        } else if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }

        controllerFuture = null;
    }
}
