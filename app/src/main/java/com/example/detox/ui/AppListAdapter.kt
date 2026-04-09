package com.example.detox.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.detox.data.AppInfo
import com.example.detox.databinding.ItemAppBinding

/**
 * RecyclerView Adapter for app list
 *
 * PURPOSE:
 * - Displays app icon, name, and block toggle
 * - Handles user interactions
 */
class AppListAdapter(
    private val onToggle: (AppInfo, Boolean) -> Unit
) : ListAdapter<AppInfo, AppListAdapter.ViewHolder>(AppDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemAppBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(app: AppInfo) {
            binding.apply {
                // App icon
                ivAppIcon.setImageDrawable(app.icon)

                // App name
                tvAppName.text = app.appName

                // Block toggle
                switchBlock.isChecked = app.isBlocked
                switchBlock.setOnCheckedChangeListener { _, isChecked ->
                    onToggle(app, isChecked)
                }
            }
        }
    }

    /**
     * DiffUtil for efficient list updates
     */
    class AppDiffCallback : DiffUtil.ItemCallback<AppInfo>() {
        override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem.packageName == newItem.packageName
        }

        override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo): Boolean {
            return oldItem == newItem
        }
    }
}