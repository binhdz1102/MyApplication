package com.example.myapplication.presentation.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.presentation.model.SongItem;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    public interface OnSongClickListener {
        void onSongClicked(int position);
    }

    private final List<SongItem> songs = new ArrayList<>();
    private final OnSongClickListener clickListener;
    private int currentSongIndex = RecyclerView.NO_POSITION;
    private boolean isPlaying;

    public SongAdapter(OnSongClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<SongItem> newSongs) {
        songs.clear();
        if (newSongs != null) {
            songs.addAll(newSongs);
        }
        currentSongIndex = RecyclerView.NO_POSITION;
        isPlaying = false;
        notifyDataSetChanged();
    }

    public void updatePlaybackState(int songIndex, boolean isPlaying) {
        int previousSongIndex = currentSongIndex;
        boolean previousPlayingState = this.isPlaying;

        currentSongIndex = songIndex;
        this.isPlaying = isPlaying;

        if (previousSongIndex != currentSongIndex) {
            notifyItemChangedIfValid(previousSongIndex);
            notifyItemChangedIfValid(currentSongIndex);
        } else if (previousPlayingState != isPlaying) {
            notifyItemChangedIfValid(currentSongIndex);
        }
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        holder.bind(songs.get(position), position == currentSongIndex, isPlaying, clickListener);
    }

    @Override
    public int getItemCount() {
        return songs.size();
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {

        private final TextView textTitle;
        private final TextView textArtist;
        private final TextView textDuration;
        private final TextView textStatus;

        SongViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_song_title);
            textArtist = itemView.findViewById(R.id.text_song_artist);
            textDuration = itemView.findViewById(R.id.text_song_duration);
            textStatus = itemView.findViewById(R.id.text_song_status);
        }

        void bind(
                SongItem song,
                boolean isCurrentSong,
                boolean isPlaying,
                OnSongClickListener clickListener
        ) {
            textTitle.setText(song.getTitle());
            textArtist.setText(song.getArtist());
            textDuration.setText(formatDuration(song.getDurationMs()));

            if (isCurrentSong) {
                textStatus.setVisibility(View.VISIBLE);
                textStatus.setText(isPlaying
                        ? R.string.song_status_playing
                        : R.string.song_status_selected);
                itemView.setActivated(true);
            } else {
                textStatus.setVisibility(View.GONE);
                itemView.setActivated(false);
            }

            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onSongClicked(getBindingAdapterPosition());
                }
            });
        }

        private String formatDuration(long durationMs) {
            if (durationMs <= 0L) {
                return "--:--";
            }

            long totalSeconds = durationMs / 1000L;
            long minutes = totalSeconds / 60L;
            long seconds = totalSeconds % 60L;
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        }
    }

    private void notifyItemChangedIfValid(int position) {
        if (position >= 0 && position < songs.size()) {
            notifyItemChanged(position);
        }
    }
}
