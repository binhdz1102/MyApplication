package com.example.myapplication.presentation.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.databinding.ItemUserBinding
import com.example.myapplication.domain.model.User

class UserAdapter(
    private val onItemClick: (User) -> Unit,
    private val onItemLongClick: (User) -> Unit,
) : ListAdapter<User, UserAdapter.UserViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return UserViewHolder(ItemUserBinding.inflate(inflater, parent, false))
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.textUserName.text = user.name
            binding.textUserId.text = binding.root.context.getString(R.string.user_id_format, user.id)
            binding.textUserAge.text = binding.root.context.getString(R.string.user_age_format, user.age)
            binding.textUserWeight.text =
                binding.root.context.getString(R.string.user_weight_format, user.weight)

            binding.root.setOnClickListener { onItemClick(user) }
            binding.root.setOnLongClickListener {
                onItemLongClick(user)
                true
            }
        }
    }

    private object DiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean = oldItem == newItem
    }
}
