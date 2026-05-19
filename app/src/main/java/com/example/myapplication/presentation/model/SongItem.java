package com.example.myapplication.presentation.model;

import android.net.Uri;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MediaMetadata;

public class SongItem {

    private final String assetPath;
    private final String title;
    private final String artist;
    private final long durationMs;
    private final String artworkUri;

    public SongItem(
            String assetPath,
            String title,
            String artist,
            long durationMs,
            String artworkUri
    ) {
        this.assetPath = assetPath;
        this.title = title;
        this.artist = artist;
        this.durationMs = durationMs;
        this.artworkUri = artworkUri;
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

    public String getArtworkUri() {
        return artworkUri;
    }

    public MediaItem toMediaItem() {
        MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder()
                .setTitle(title)
                .setDisplayTitle(title)
                .setArtist(artist)
                .setDurationMs(durationMs);

        if (artworkUri != null && !artworkUri.trim().isEmpty()) {
            metadataBuilder.setArtworkUri(Uri.parse(artworkUri));
        }

        return new MediaItem.Builder()
                .setMediaId(assetPath)
                .setUri(Uri.parse("asset:///" + assetPath))
                .setMediaMetadata(metadataBuilder.build())
                .build();
    }
}
