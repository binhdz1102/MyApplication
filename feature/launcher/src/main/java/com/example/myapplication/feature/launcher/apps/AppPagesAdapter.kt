package com.example.myapplication.feature.launcher.apps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.core.model.AppShortcut
import com.example.myapplication.feature.launcher.databinding.ItemAppPageBinding

class AppPagesAdapter(
    private val onAppClick: (AppShortcut) -> Unit,
) : RecyclerView.Adapter<AppPagesAdapter.AppPageViewHolder>() {
    private val pages = mutableListOf<List<AppShortcut>>()

    fun submitPages(newPages: List<List<AppShortcut>>) {
        pages.clear()
        pages.addAll(newPages)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AppPageViewHolder {
        val binding =
            ItemAppPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return AppPageViewHolder(binding, onAppClick)
    }

    override fun getItemCount(): Int = pages.size

    override fun onBindViewHolder(
        holder: AppPageViewHolder,
        position: Int,
    ) {
        holder.bind(pages[position])
    }

    class AppPageViewHolder(
        private val binding: ItemAppPageBinding,
        onAppClick: (AppShortcut) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {
        private val shortcutAdapter = AppShortcutAdapter(onAppClick)

        init {
            binding.appsGrid.layoutManager = GridLayoutManager(binding.root.context, 4)
            binding.appsGrid.adapter = shortcutAdapter
        }

        fun bind(items: List<AppShortcut>) {
            shortcutAdapter.submitItems(items)
        }
    }
}
