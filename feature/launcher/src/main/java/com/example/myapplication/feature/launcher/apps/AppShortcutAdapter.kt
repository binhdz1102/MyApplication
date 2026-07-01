package com.example.myapplication.feature.launcher.apps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.core.model.AppShortcut
import com.example.myapplication.feature.launcher.R
import com.example.myapplication.feature.launcher.databinding.ItemAppShortcutBinding
import com.example.myapplication.core.ui.R as CoreUiR

class AppShortcutAdapter(
    private val onAppClick: (AppShortcut) -> Unit,
) : RecyclerView.Adapter<AppShortcutAdapter.AppShortcutViewHolder>() {
    private val items = mutableListOf<AppShortcut>()
    private val iconBackgrounds =
        listOf<Int>(
            CoreUiR.color.launcher_icon_bg_1,
            CoreUiR.color.launcher_icon_bg_2,
            CoreUiR.color.launcher_icon_bg_3,
            CoreUiR.color.launcher_icon_bg_4,
        )

    fun submitItems(newItems: List<AppShortcut>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): AppShortcutViewHolder {
        val binding =
            ItemAppShortcutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return AppShortcutViewHolder(binding)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(
        holder: AppShortcutViewHolder,
        position: Int,
    ) {
        holder.bind(items[position], iconBackgrounds[position % iconBackgrounds.size], onAppClick)
    }

    class AppShortcutViewHolder(
        private val binding: ItemAppShortcutBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: AppShortcut,
            backgroundColorRes: Int,
            onAppClick: (AppShortcut) -> Unit,
        ) {
            binding.appLabel.text = item.label
            binding.appIcon.setImageResource(item.iconRes)
            binding.iconShell.background =
                binding.root.context.getDrawable(R.drawable.bg_app_icon)?.mutate()?.also {
                    DrawableCompat.setTint(it, ContextCompat.getColor(binding.root.context, backgroundColorRes))
                }
            binding.root.setOnClickListener { onAppClick(item) }
        }
    }
}
