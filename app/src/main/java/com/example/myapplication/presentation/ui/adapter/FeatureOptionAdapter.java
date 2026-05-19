package com.example.myapplication.presentation.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.presentation.model.FeatureOption;

import java.util.ArrayList;
import java.util.List;

public class FeatureOptionAdapter extends RecyclerView.Adapter<FeatureOptionAdapter.FeatureOptionViewHolder> {

    public interface OnFeatureClickListener {
        void onFeatureClicked(FeatureOption option);
    }

    private final List<FeatureOption> items = new ArrayList<>();
    private final OnFeatureClickListener clickListener;

    public FeatureOptionAdapter(OnFeatureClickListener clickListener) {
        this.clickListener = clickListener;
    }

    public void submitList(List<FeatureOption> featureOptions) {
        items.clear();
        if (featureOptions != null) {
            items.addAll(featureOptions);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FeatureOptionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feature_option, parent, false);
        return new FeatureOptionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeatureOptionViewHolder holder, int position) {
        holder.bind(items.get(position), clickListener);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class FeatureOptionViewHolder extends RecyclerView.ViewHolder {

        private final TextView textTitle;
        private final TextView textMessage;

        FeatureOptionViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_feature_title);
            textMessage = itemView.findViewById(R.id.text_feature_message);
        }

        void bind(FeatureOption option, OnFeatureClickListener clickListener) {
            textTitle.setText(option.getTitleRes());
            textMessage.setText(option.getMessageRes());
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onFeatureClicked(option);
                }
            });
        }
    }
}
