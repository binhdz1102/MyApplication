package com.example.myapplication.presentation.media;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myapplication.R;
import com.example.myapplication.presentation.model.SongItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class SongAssetLoader {

    private static final String LOG_TAG = "SongAssetLoader";

    private SongAssetLoader() {
    }

    @NonNull
    public static List<SongItem> loadSongs(@NonNull Context context) {
        List<SongItem> loadedSongs = new ArrayList<>();

        try {
            String[] assetNames = context.getAssets().list("");
            if (assetNames == null) {
                return loadedSongs;
            }

            Arrays.sort(assetNames, String.CASE_INSENSITIVE_ORDER);
            for (String assetName : assetNames) {
                if (!assetName.toLowerCase(Locale.ROOT).endsWith(".mp3")) {
                    continue;
                }
                loadedSongs.add(readSongMetadata(context, assetName));
            }
        } catch (IOException exception) {
            Log.e(LOG_TAG, "Unable to load songs from assets.", exception);
        }

        return loadedSongs;
    }

    @NonNull
    private static SongItem readSongMetadata(@NonNull Context context, @NonNull String assetName) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try (AssetFileDescriptor assetFileDescriptor = context.getAssets().openFd(assetName)) {
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
                    context.getString(R.string.label_unknown_artist)
            );
            long durationMs = parseDuration(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            );
            return new SongItem(
                    assetName,
                    title,
                    artist,
                    durationMs,
                    buildArtworkUri(context, assetName)
            );
        } catch (IOException | RuntimeException exception) {
            Log.w(LOG_TAG, "Unable to read metadata for asset: " + assetName, exception);
            return new SongItem(
                    assetName,
                    formatAssetName(assetName),
                    context.getString(R.string.label_unknown_artist),
                    0L,
                    buildArtworkUri(context, assetName)
            );
        } finally {
            try {
                retriever.release();
            } catch (IOException exception) {
                Log.w(LOG_TAG, "Unable to release metadata retriever.", exception);
            }
        }
    }

    @NonNull
    private static String readMetadata(
            @NonNull MediaMetadataRetriever retriever,
            int metadataKey,
            @NonNull String fallbackValue
    ) {
        String value = retriever.extractMetadata(metadataKey);
        if (value == null || value.trim().isEmpty()) {
            return fallbackValue;
        }
        return value.trim();
    }

    private static long parseDuration(String durationValue) {
        if (durationValue == null || durationValue.trim().isEmpty()) {
            return 0L;
        }

        try {
            return Long.parseLong(durationValue);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    @NonNull
    private static String formatAssetName(@NonNull String assetName) {
        String fileName = assetName;
        int extensionIndex = fileName.lastIndexOf('.');
        if (extensionIndex > 0) {
            fileName = fileName.substring(0, extensionIndex);
        }

        String[] words = fileName.split("_");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) {
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

    @NonNull
    private static String buildArtworkUri(@NonNull Context context, @NonNull String assetName) {
        String resourceName = assetName.hashCode() % 2 == 0
                ? "ic_launcher"
                : "ic_launcher_round";
        Uri uri = new Uri.Builder()
                .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                .authority(context.getPackageName())
                .appendPath("mipmap")
                .appendPath(resourceName)
                .build();
        return uri.toString();
    }
}
