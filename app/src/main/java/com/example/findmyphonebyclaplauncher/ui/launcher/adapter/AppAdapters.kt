package com.example.findmyphonebyclaplauncher.ui.launcher.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.findmyphonebyclaplauncher.data.model.AppInfo
import com.example.findmyphonebyclaplauncher.databinding.ItemAppIconBinding
import com.example.findmyphonebyclaplauncher.databinding.ItemAppIconCompactBinding
import com.example.findmyphonebyclaplauncher.databinding.ItemDockIconBinding
import com.example.findmyphonebyclaplauncher.databinding.ItemWorkspaceIconBinding

class AppIconAdapter(
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, View) -> Unit = { _, _ -> }
) : ListAdapter<AppInfo, AppIconAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAppIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemAppIconBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppInfo) {
            binding.ivIcon.setImageDrawable(item.icon)
            binding.tvLabel.text = item.label
            binding.root.setOnClickListener {
                it.animate().scaleX(0.9f).scaleY(0.9f).setDuration(60).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
                    onClick(item)
                }.start()
            }
            binding.root.setOnLongClickListener {
                onLongClick(item, it)
                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.packageName == newItem.packageName && oldItem.activityName == newItem.activityName

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.label == newItem.label &&
                    oldItem.isFavorite == newItem.isFavorite &&
                    oldItem.canUninstall == newItem.canUninstall
        }
    }
}

class CompactAppIconAdapter(
    private val onClick: (AppInfo) -> Unit
) : ListAdapter<AppInfo, CompactAppIconAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemAppIconCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemAppIconCompactBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppInfo) {
            binding.ivIcon.setImageDrawable(item.icon)
            binding.tvLabel.text = item.label
            binding.root.setOnClickListener { onClick(item) }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.label == newItem.label
        }
    }
}

class DockAdapter(
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, View) -> Unit = { _, _ -> }
) : ListAdapter<AppInfo, DockAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemDockIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemDockIconBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppInfo) {
            binding.ivIcon.setImageDrawable(item.icon)
            binding.root.setOnTouchListener(DisallowParentInterceptTouchListener)
            binding.root.setOnClickListener {
                it.animate().scaleX(0.88f).scaleY(0.88f).setDuration(50).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                    onClick(item)
                }.start()
            }
            binding.root.setOnLongClickListener {
                onLongClick(item, it)
                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.label == newItem.label &&
                    oldItem.isFavorite == newItem.isFavorite &&
                    oldItem.canUninstall == newItem.canUninstall
        }
    }
}

class WorkspaceIconAdapter(
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (AppInfo, View) -> Unit = { _, _ -> }
) : ListAdapter<AppInfo, WorkspaceIconAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWorkspaceIconBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemWorkspaceIconBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AppInfo) {
            binding.ivIcon.setImageDrawable(item.icon)
            binding.root.setOnTouchListener(DisallowParentInterceptTouchListener)
            binding.root.setOnClickListener {
                it.animate().scaleX(0.88f).scaleY(0.88f).setDuration(50).withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(90).start()
                    onClick(item)
                }.start()
            }
            binding.root.setOnLongClickListener {
                onLongClick(item, it)
                true
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<AppInfo>() {
            override fun areItemsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.packageName == newItem.packageName

            override fun areContentsTheSame(oldItem: AppInfo, newItem: AppInfo) =
                oldItem.label == newItem.label &&
                    oldItem.isFavorite == newItem.isFavorite &&
                    oldItem.canUninstall == newItem.canUninstall
        }
    }
}

@SuppressLint("ClickableViewAccessibility")
private object DisallowParentInterceptTouchListener : View.OnTouchListener {
    override fun onTouch(v: View, event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> setDisallowIntercept(v, true)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> setDisallowIntercept(v, false)
        }
        return false
    }

    private fun setDisallowIntercept(v: View, disallow: Boolean) {
        var parent = v.parent
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow)
            parent = parent.parent
        }
    }
}
