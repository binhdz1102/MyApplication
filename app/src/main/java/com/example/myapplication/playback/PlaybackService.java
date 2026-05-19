package com.example.myapplication.playback;

import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.example.myapplication.MainActivity;

public class PlaybackService extends MediaSessionService {

    private ExoPlayer player;
    private MediaSession mediaSession;

    @Override
    public void onCreate() {
        super.onCreate();

        player = new ExoPlayer.Builder(this).build();
        Intent launchIntent = new Intent(this, MainActivity.class);
        launchIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent sessionActivity = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );
        mediaSession = new MediaSession.Builder(this, player)
                .setId("background-playback-service")
                .setSessionActivity(sessionActivity)
                .build();
    }

    @Override
    public @Nullable MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onDestroy() {
        if (player != null) {
            player.release();
            player = null;
        }

        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }

        super.onDestroy();
    }
}
