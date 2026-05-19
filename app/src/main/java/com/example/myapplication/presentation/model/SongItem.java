package com.example.myapplication.presentation.model;

import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

public class SongItem {

    private final String assetPath;
    private final String title;
    private final String artist;
    private final long durationMs;

    public SongItem(String assetPath, String title, String artist, long durationMs) {
        this.assetPath = assetPath;
        this.title = title;
        this.artist = artist;
        this.durationMs = durationMs;
    }

    public String getAssetPath() {
        return assetPath;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public MediaItem toMediaItem() {
        return new MediaItem.Builder()
                .setMediaId(assetPath)
                .setUri(Uri.parse("asset:///" + assetPath))
                .setMediaMetadata(
                        new MediaMetadata.Builder()
                                .setTitle(title)
                                .setArtist(artist)
                                .build()
                )
                .build();
    }
}
