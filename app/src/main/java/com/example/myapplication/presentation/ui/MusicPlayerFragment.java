package com.example.myapplication.presentation.ui;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.databinding.FragmentMusicPlayerBinding;
import com.example.myapplication.presentation.model.SongItem;
import com.example.myapplication.presentation.ui.adapter.SongAdapter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class MusicPlayerFragment extends Fragment {

    public static final String TAG = "MusicPlayerFragment";

    private static final String LOG_TAG = "MusicPlayerFragment";
    private static final long PROGRESS_UPDATE_INTERVAL_MS = 500L;

    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final List<SongItem> songs = new ArrayList<>();
    private final SongAdapter songAdapter = new SongAdapter(this::playSongAt);

    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            updateProgressUi();
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
        }

        @Override
        public void onIsPlayingChanged(boolean isPlaying) {
            updateSongSelection();
            updatePlayerUi();
        }

        @Override
        public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
            updateSongSelection();
            updatePlayerUi();
        }
    };

    private FragmentMusicPlayerBinding binding;
    private ScreenConfigurationHost screenConfigurationHost;
    private ExoPlayer player;

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
        binding = FragmentMusicPlayerBinding.inflate(inflater, container, false);
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

        songs.clear();
        songs.addAll(loadSongsFromAssets());
        songAdapter.submitList(songs);
        binding.textEmptySongs.setVisibility(songs.isEmpty() ? View.VISIBLE : View.GONE);
        binding.recyclerSongs.setVisibility(songs.isEmpty() ? View.GONE : View.VISIBLE);

        if (!songs.isEmpty()) {
            initializePlayer();
        } else {
            updatePlayerUi();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (screenConfigurationHost != null) {
            screenConfigurationHost.updateScreenConfiguration(
                    new ScreenConfiguration(
                            R.string.title_music_player,
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
        }
        super.onStop();
    }

    @Override
    public void onDestroyView() {
        releasePlayer();
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
    private void initializePlayer() {
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
        progressHandler.post(progressRunnable);
        updateSongSelection();
        updatePlayerUi();
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
    }

    private void pausePlayback() {
        if (player == null) {
            return;
        }
        player.pause();
        updateSongSelection();
        updatePlayerUi();
    }

    private void stopPlayback() {
        if (player == null) {
            return;
        }
        player.stop();
        updateSongSelection();
        updatePlayerUi();
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

        int previousSongIndex = Math.max(currentSongIndex - 1, 0);
        playSongAt(previousSongIndex);
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

        int nextSongIndex = Math.min(currentSongIndex + 1, songs.size() - 1);
        playSongAt(nextSongIndex);
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
            }
        };
    }

    private List<SongItem> loadSongsFromAssets() {
        List<SongItem> loadedSongs = new ArrayList<>();

        try {
            String[] assetNames = requireContext().getAssets().list("");
            if (assetNames == null) {
                return loadedSongs;
            }

            Arrays.sort(assetNames, String.CASE_INSENSITIVE_ORDER);
            for (String assetName : assetNames) {
                if (!assetName.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
                    continue;
                }
                loadedSongs.add(readSongMetadata(assetName));
            }
        } catch (IOException exception) {
            Log.e(LOG_TAG, "Unable to load songs from assets.", exception);
        }

        return loadedSongs;
    }

    private SongItem readSongMetadata(String assetName) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try (AssetFileDescriptor assetFileDescriptor = requireContext().getAssets().openFd(assetName)) {
            retriever.setDataSource(
                    assetFileDescriptor.getFileDescriptor(),
                    assetFileDescriptor.getStartOffset(),
                    assetFileDescriptor.getLength()
            );

            String title = readMetadata(
                    retriever,
                    MediaMetadataRetriever.METADATA_KEY_TITLE,
                    formatAssetName(assetName)
            );
            String artist = readMetadata(
                    retriever,
                    MediaMetadataRetriever.METADATA_KEY_ARTIST,
                    getString(R.string.label_unknown_artist)
            );
            long durationMs = parseDuration(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            );
            return new SongItem(assetName, title, artist, durationMs);
        } catch (IOException | RuntimeException exception) {
            Log.w(LOG_TAG, "Unable to read metadata for asset: " + assetName, exception);
            return new SongItem(
                    assetName,
                    formatAssetName(assetName),
                    getString(R.string.label_unknown_artist),
                    0L
            );
        } finally {
            try {
                retriever.release();
            } catch (IOException exception) {
                Log.w(LOG_TAG, "Unable to release metadata retriever.", exception);
            }
        }
    }

    private String readMetadata(
            MediaMetadataRetriever retriever,
            int metadataKey,
            String fallbackValue
    ) {
        String value = retriever.extractMetadata(metadataKey);
        if (value == null || value.trim().isEmpty()) {
            return fallbackValue;
        }
        return value.trim();
    }

    private long parseDuration(@Nullable String durationValue) {
        if (durationValue == null || durationValue.trim().isEmpty()) {
            return 0L;
        }

        try {
            return Long.parseLong(durationValue);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    private String formatAssetName(String assetName) {
        String fileName = assetName;
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileName = fileName.substring(0, extensionIndex);
        }

        String[] words = fileName.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word == null || word.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.length() == 0 ? assetName : builder.toString();
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

    private void releasePlayer() {
        progressHandler.removeCallbacks(progressRunnable);
        if (player != null) {
            player.removeListener(playerListener);
            player.release();
            player = null;
        }
    }
}
