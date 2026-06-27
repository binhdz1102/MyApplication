package com.example.myapplication.presentation.ui.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.databinding.ItemUserBinding;
import com.example.myapplication.domain.model.User;

public class UserAdapter extends ListAdapter<User, UserAdapter.UserViewHolder> {

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public interface OnUserLongClickListener {
        void onUserLongClick(User user);
    }

    private static final DiffUtil.ItemCallback<User> DIFF_CALLBACK = new DiffUtil.ItemCallback<User>() {
        @Override
        public boolean areItemsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull User oldItem, @NonNull User newItem) {
            return oldItem.equals(newItem);
        }
    };

    private final OnUserClickListener onItemClick;
    private final OnUserLongClickListener onItemLongClick;

    public UserAdapter(OnUserClickListener onItemClick, OnUserLongClickListener onItemLongClick) {
        super(DIFF_CALLBACK);
        this.onItemClick = onItemClick;
        this.onItemLongClick = onItemLongClick;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new UserViewHolder(ItemUserBinding.inflate(inflater, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    class UserViewHolder extends RecyclerView.ViewHolder {

        private final ItemUserBinding binding;

        UserViewHolder(ItemUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user) {
            binding.textUserName.setText(user.getName());
            binding.textUserId.setText(binding.getRoot().getContext().getString(R.string.user_id_format, user.getId()));
            binding.textUserAge.setText(binding.getRoot().getContext().getString(R.string.user_age_format, user.getAge()));
            binding.textUserWeight.setText(binding.getRoot().getContext().getString(R.string.user_weight_format, user.getWeight()));

            binding.getRoot().setOnClickListener(v -> onItemClick.onUserClick(user));
            binding.getRoot().setOnLongClickListener(v -> {
                onItemLongClick.onUserLongClick(user);
                return true;
            });
        }
    }
}
