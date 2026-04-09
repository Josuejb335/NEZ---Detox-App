package com.example.detox.ui

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.detox.databinding.ItemAppBinding

/**
 * Data class for app list item
 */
data class AppItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val isBlocked: Boolean,
    val attemptCount: Int
)

/**
 * RecyclerView adapter for app list
 */
class AppListAdapter(
    private val onToggle: (packageName: String, appName: String, isBlocked: Boolean) -> Unit
) : ListAdapter<AppItem, AppListAdapter.ViewHolder>(AppDiffCallback()) {

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

        fun bind(item: AppItem) {
            binding.apply {
                ivAppIcon.setImageDrawable(item.icon)
                tvAppName.text = item.appName

                // Show attempt count if > 0
                if (item.attemptCount > 0) {
                    tvAttempts.text = "Tried ${item.attemptCount} times"
                    tvAttempts.visibility = android.view.View.VISIBLE
                } else {
                    tvAttempts.visibility = android.view.View.GONE
                }

                // Handle toggle
                switchBlock.setOnCheckedChangeListener(null)
                switchBlock.isChecked = item.isBlocked
                switchBlock.setOnCheckedChangeListener { _, isChecked ->
                    onToggle(item.packageName, item.appName, isChecked)
                }
            }
        }
    }

    class AppDiffCallback : DiffUtil.ItemCallback<AppItem>() {
        override fun areItemsTheSame(old: AppItem, new: AppItem): Boolean {
            return old.packageName == new.packageName
        }

        override fun areContentsTheSame(old: AppItem, new: AppItem): Boolean {
            return old == new
        }
    }
}