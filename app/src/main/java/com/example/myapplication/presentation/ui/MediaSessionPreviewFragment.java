package com.example.myapplication.presentation.ui;

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
import androidx.annotation.OptIn;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.session.MediaController;
import androidx.media3.session.MediaSession;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentMediaSessionPreviewBinding;
import com.example.myapplication.presentation.media.SongAssetLoader;
import com.example.myapplication.presentation.model.SongItem;
import com.example.myapplication.presentation.ui.adapter.SongAdapter;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class MediaSessionPreviewFragment extends Fragment {

    public static final String TAG = "MediaSessionPreviewFragment";
    private static final String SESSION_ID = "media-session-preview";
    private static final long PROGRESS_UPDATE_INTERVAL_MS = 500L;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final List<SongItem> songs = new ArrayList<>();
    private final SongAdapter songAdapter = new SongAdapter(this::playSongAt);

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgressUi();
            updateSessionPreviewUi();
            if (binding != null) {
                progressHandler.postDelayed(this, PROGRESS_UPDATE_INTERVAL_MS);
            }
        }
    };

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            updateSongSelection();
            updatePlayerUi();
            updateSessionPreviewUi();
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            updateSongSelection();
            updatePlayerUi();
            updateSessionPreviewUi();
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            updateSongSelection();
            updatePlayerUi();
            updateSessionPreviewUi();
        }
    };

    private final Player.Listener controllerListener = new Player.Listener() {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            updateSessionPreviewUi();
        }

        @Override
        public void onMediaMetadataChanged(MediaMetadata mediaMetadata) {
            updateSessionPreviewUi();
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            updateSessionPreviewUi();
        }
    };

    private FragmentMediaSessionPreviewBinding binding;
    private ScreenConfigurationHost screenConfigurationHost;
    private ExoPlayer player;
    private MediaSession mediaSession;
    private ListenableFuture<MediaController> controllerFuture;
    private MediaController sessionController;

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
        binding = FragmentMediaSessionPreviewBinding.inflate(inflater, container, false);
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
        binding.buttonStop.setOnClickListener(v -> stopPlayback());
        binding.buttonPrevious.setOnClickListener(v -> playPrevious());
        binding.buttonNext.setOnClickListener(v -> playNext());

        binding.textSessionIdValue.setText(R.string.session_id_value);

        songs.clear();
        songs.addAll(SongAssetLoader.loadSongs(requireContext()));
        songAdapter.submitList(songs);

        if (!songs.isEmpty()) {
            initializePlayerAndSession();
        } else {
            updatePlayerUi();
            updateSessionPreviewUi();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (screenConfigurationHost != null) {
            screenConfigurationHost.updateScreenConfiguration(
                    new ScreenConfiguration(
                            R.string.title_media_session_preview,
                            true,
                            false,
                            false
                    )
            );
        }
    }

    @Override
    public void onStop() {
        if (player != null && player.isPlaying()) {
            player.pause();
            updatePlayerUi();
            updateSessionPreviewUi();
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        releaseResources();
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

    @OptIn(markerClass = UnstableApi.class)
    private void initializePlayerAndSession() {
        if (player != null) {
            return;
        }

        player = new ExoPlayer.Builder(requireContext())
                .setMediaSourceFactory(
                        new DefaultMediaSourceFactory(new DefaultDataSource.Factory(requireContext()))
                )
                .build();
        player.addListener(playerListener);

        List<MediaItem> mediaItems = new ArrayList<>();
        for (SongItem song : songs) {
            mediaItems.add(song.toMediaItem());
        }
        player.setMediaItems(mediaItems, 0, 0L);
        player.prepare();

        mediaSession = new MediaSession.Builder(requireContext(), player)
                .setId(SESSION_ID)
                .build();

        controllerFuture = new MediaController.Builder(requireContext(), mediaSession.getToken())
                .buildAsync();
        controllerFuture.addListener(this::attachControllerWhenReady, ContextCompat.getMainExecutor(requireContext()));

        progressHandler.post(progressRunnable);
        updateSongSelection();
        updatePlayerUi();
        updateSessionPreviewUi();
    }

    private void attachControllerWhenReady() {
        if (!isAdded() || controllerFuture == null) {
            return;
        }

        try {
            sessionController = controllerFuture.get();
            sessionController.addListener(controllerListener);
        } catch (ExecutionException | InterruptedException exception) {
            if (exception instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
        updateSessionPreviewUi();
    }

    private void playSongAt(int position) {
        if (player == null || position == RecyclerView.NO_POSITION || position < 0 || position >= songs.size()) {
            return;
        }

        player.seekToDefaultPosition(position);
        if (player.getPlaybackState() == Player.STATE_IDLE) {
            player.prepare();
        }
        player.play();
        updateSongSelection();
        updatePlayerUi();
        updateSessionPreviewUi();
    }

    private void playCurrentSelection() {
        if (player == null || songs.isEmpty()) {
            return;
        }

        int currentSongIndex = getCurrentSongIndex();
        if (currentSongIndex == C.INDEX_UNSET) {
            player.seekToDefaultPosition(0);
        } else if (player.getPlaybackState() == Player.STATE_ENDED) {
            player.seekTo(currentSongIndex, 0L);
        }

        if (player.getPlaybackState() == Player.STATE_IDLE) {
            player.prepare();
        }

        player.play();
        updateSongSelection();
        updatePlayerUi();
        updateSessionPreviewUi();
    }

    private void pausePlayback() {
        if (player == null) {
            return;
        }
        player.pause();
        updateSongSelection();
        updatePlayerUi();
        updateSessionPreviewUi();
    }

    private void stopPlayback() {
        if (player == null) {
            return;
        }
        player.stop();
        updateSongSelection();
        updatePlayerUi();
        updateSessionPreviewUi();
    }

    private void playPrevious() {
        if (player == null || songs.isEmpty()) {
            return;
        }

        int currentSongIndex = getCurrentSongIndex();
        if (currentSongIndex == C.INDEX_UNSET) {
            playSongAt(0);
            return;
        }

        playSongAt(Math.max(currentSongIndex - 1, 0));
    }

    private void playNext() {
        if (player == null || songs.isEmpty()) {
            return;
        }

        int currentSongIndex = getCurrentSongIndex();
        if (currentSongIndex == C.INDEX_UNSET) {
            playSongAt(0);
            return;
        }

        playSongAt(Math.min(currentSongIndex + 1, songs.size() - 1));
    }

    private void updatePlayerUi() {
        if (binding == null) {
            return;
        }

        SongItem currentSong = getCurrentSong();
        boolean hasSongs = !songs.isEmpty();
        boolean hasCurrentSong = currentSong != null;
        int currentSongIndex = getCurrentSongIndex();
        long currentDuration = resolveCurrentSongDuration(currentSong);

        binding.textNowPlayingTitle.setText(
                hasCurrentSong ? currentSong.getTitle() : getString(R.string.label_not_available)
        );
        binding.textNowPlayingArtist.setText(
                hasCurrentSong ? currentSong.getArtist() : getString(R.string.label_unknown_artist)
        );

        binding.buttonPlay.setEnabled(hasSongs);
        binding.buttonPause.setEnabled(player != null && player.isPlaying());
        binding.buttonStop.setEnabled(player != null && hasSongs
                && player.getPlaybackState() != Player.STATE_IDLE);
        binding.buttonPrevious.setEnabled(hasCurrentSong && currentSongIndex > 0);
        binding.buttonNext.setEnabled(hasCurrentSong && currentSongIndex < songs.size() - 1);
        binding.seekPlayback.setEnabled(hasCurrentSong);

        binding.textDurationValue.setText(formatDuration(currentDuration));
        updateProgressUi();
    }

    private void updateSessionPreviewUi() {
        if (binding == null) {
            return;
        }

        boolean isControllerConnected = sessionController != null && sessionController.isConnected();
        boolean isSessionActive = mediaSession != null && isControllerConnected;
        binding.textSessionActiveValue.setText(
                isSessionActive ? R.string.status_active : R.string.status_inactive
        );
        binding.textControllerConnectedValue.setText(
                isControllerConnected ? R.string.status_connected : R.string.status_disconnected
        );

        int connectedControllerCount = mediaSession == null
                ? 0
                : mediaSession.getConnectedControllers().size();
        binding.textConnectedControllersValue.setText(String.valueOf(connectedControllerCount));

        MediaMetadata sessionMetadata = sessionController == null
                ? MediaMetadata.EMPTY
                : sessionController.getMediaMetadata();
        CharSequence title = sessionMetadata.title;
        CharSequence artist = sessionMetadata.artist;
        long durationMs = sessionController == null
                ? resolveCurrentSongDuration(getCurrentSong())
                : resolveControllerDuration();

        binding.textSessionMetadataTitleValue.setText(
                title == null || title.length() == 0
                        ? getString(R.string.label_not_available)
                        : title
        );
        binding.textSessionMetadataArtistValue.setText(
                artist == null || artist.length() == 0
                        ? getString(R.string.label_unknown_artist)
                        : artist
        );
        binding.textSessionMetadataDurationValue.setText(formatDuration(durationMs));
    }

    private void updateSongSelection() {
        songAdapter.updatePlaybackState(getCurrentSongIndex(), player != null && player.isPlaying());
    }

    private void updateProgressUi() {
        if (binding == null) {
            return;
        }

        SongItem currentSong = getCurrentSong();
        long durationMs = resolveCurrentSongDuration(currentSong);
        long positionMs = player == null ? 0L : Math.max(player.getCurrentPosition(), 0L);
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
        if (player == null || songs.isEmpty()) {
            return RecyclerView.NO_POSITION;
        }

        int currentIndex = player.getCurrentMediaItemIndex();
        if (currentIndex == C.INDEX_UNSET || currentIndex < 0 || currentIndex >= songs.size()) {
            return RecyclerView.NO_POSITION;
        }
        return currentIndex;
    }

    @Nullable
    private SongItem getCurrentSong() {
        int currentSongIndex = getCurrentSongIndex();
        if (currentSongIndex == RecyclerView.NO_POSITION) {
            return songs.isEmpty() ? null : songs.get(0);
        }
        return songs.get(currentSongIndex);
    }

    private long resolveCurrentSongDuration(@Nullable SongItem currentSong) {
        long durationMs = player == null ? C.TIME_UNSET : player.getDuration();
        if (durationMs == C.TIME_UNSET || durationMs < 0L) {
            return currentSong == null ? 0L : Math.max(currentSong.getDurationMs(), 0L);
        }
        return durationMs;
    }

    private long resolveControllerDuration() {
        if (sessionController == null) {
            return 0L;
        }

        long durationMs = sessionController.getDuration();
        if (durationMs == C.TIME_UNSET || durationMs < 0L) {
            return resolveCurrentSongDuration(getCurrentSong());
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
                if (player == null || songs.isEmpty()) {
                    return;
                }
                if (player.getPlaybackState() == Player.STATE_IDLE) {
                    player.prepare();
                }
                player.seekTo(seekBar.getProgress());
                updateProgressUi();
                updateSessionPreviewUi();
            }
        };
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

    private void releaseResources() {
        progressHandler.removeCallbacks(progressRunnable);

        if (sessionController != null) {
            sessionController.removeListener(controllerListener);
            sessionController.release();
            sessionController = null;
        } else if (controllerFuture != null) {
            MediaController.releaseFuture(controllerFuture);
        }
        controllerFuture = null;

        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }

        if (player != null) {
            player.removeListener(playerListener);
            player.release();
            player = null;
        }
    }
}
