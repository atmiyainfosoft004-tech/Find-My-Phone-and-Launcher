package com.example.findmyphonebyclaplauncher.ui.search

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.findmyphonebyclaplauncher.ads.LauncherAdsHelper
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.data.model.BlogPost
import com.example.findmyphonebyclaplauncher.data.model.GoogleSearchFeedItem
import com.example.findmyphonebyclaplauncher.databinding.ItemBlogAdPlaceholderBinding
import com.example.findmyphonebyclaplauncher.databinding.ItemBlogPostBinding
import com.google.android.gms.ads.nativead.NativeAd

class GoogleSearchFeedAdapter(
    private val activity: Activity,
    private val onBlogClick: (BlogPost) -> Unit
) : ListAdapter<GoogleSearchFeedItem, RecyclerView.ViewHolder>(DIFF) {

    private val nativeAds = mutableMapOf<Int, NativeAd>()
    private val loadingIds = mutableSetOf<Int>()
    private val failedIds = mutableSetOf<Int>()
    private var destroyed = false

    override fun getItemViewType(position: Int): Int = when (getItem(position)) {
        is GoogleSearchFeedItem.Blog -> TYPE_BLOG
        is GoogleSearchFeedItem.AdPlaceholder -> TYPE_AD
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_BLOG -> BlogVH(ItemBlogPostBinding.inflate(inflater, parent, false))
            else -> AdVH(ItemBlogAdPlaceholderBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is GoogleSearchFeedItem.Blog -> (holder as BlogVH).bind(item.post, onBlogClick)
            is GoogleSearchFeedItem.AdPlaceholder -> (holder as AdVH).bind(item.id)
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is AdVH) holder.unbind()
        super.onViewRecycled(holder)
    }

    fun destroyAds() {
        destroyed = true
        nativeAds.values.forEach { ad -> runCatching { ad.destroy() } }
        nativeAds.clear()
        loadingIds.clear()
        failedIds.clear()
    }

    private fun AdVH.bind(adId: Int) {
        boundAdId = adId
        if (!AdsConfigManager.config.canShowNative || adId in failedIds) {
            hideSlot()
            return
        }
        binding.root.visibility = View.VISIBLE
        binding.nativeAdShimmerFrameLayout.visibility = View.VISIBLE
        binding.nativeAdFrameLayout.visibility = View.GONE
        binding.nativeAdFrameLayout.removeAllViews()

        val cached = nativeAds[adId]
        if (cached != null) {
            LauncherAdsHelper.bindGoogleSearchNative(
                activity,
                binding.nativeAdFrameLayout,
                binding.nativeAdShimmerFrameLayout,
                cached
            )
            return
        }
        if (adId in loadingIds) return
        loadingIds += adId
        LauncherAdsHelper.loadGoogleSearchNative(
            activity,
            onLoaded = { nativeAd ->
                if (destroyed) {
                    nativeAd.destroy()
                    return@loadGoogleSearchNative
                }
                nativeAds[adId] = nativeAd
                loadingIds -= adId
                notifyAdSlot(adId)
            },
            onFailed = {
                if (destroyed) return@loadGoogleSearchNative
                loadingIds -= adId
                failedIds += adId
                notifyAdSlot(adId)
            }
        )
    }

    private fun notifyAdSlot(adId: Int) {
        val position = currentList.indexOfFirst {
            it is GoogleSearchFeedItem.AdPlaceholder && it.id == adId
        }
        if (position >= 0) notifyItemChanged(position)
    }

    class BlogVH(private val binding: ItemBlogPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: BlogPost, onClick: (BlogPost) -> Unit) {
            binding.tvTitle.text = post.title
            binding.tvSource.text = post.source
            binding.tvTime.text = post.timeLabel

            val cover = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * binding.root.resources.displayMetrics.density
                setColor(post.imageColor)
            }
            binding.ivCover.setImageDrawable(null)
            binding.ivCover.background = cover

            val dot = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 3f * binding.root.resources.displayMetrics.density
                setColor(ColorUtils.blendARGB(post.imageColor, 0xFF4285F4.toInt(), 0.35f))
            }
            binding.sourceDot.background = dot

            binding.root.setOnClickListener { onClick(post) }
        }
    }

    class AdVH(val binding: ItemBlogAdPlaceholderBinding) : RecyclerView.ViewHolder(binding.root) {
        var boundAdId: Int? = null

        fun unbind() {
            boundAdId = null
            binding.nativeAdFrameLayout.removeAllViews()
        }

        fun hideSlot() {
            binding.nativeAdFrameLayout.removeAllViews()
            binding.nativeAdFrameLayout.visibility = View.GONE
            binding.nativeAdShimmerFrameLayout.visibility = View.GONE
            binding.root.visibility = View.GONE
        }
    }

    companion object {
        private const val TYPE_BLOG = 1
        private const val TYPE_AD = 2

        private val DIFF = object : DiffUtil.ItemCallback<GoogleSearchFeedItem>() {
            override fun areItemsTheSame(
                oldItem: GoogleSearchFeedItem,
                newItem: GoogleSearchFeedItem
            ): Boolean = when {
                oldItem is GoogleSearchFeedItem.Blog && newItem is GoogleSearchFeedItem.Blog ->
                    oldItem.post.id == newItem.post.id
                oldItem is GoogleSearchFeedItem.AdPlaceholder && newItem is GoogleSearchFeedItem.AdPlaceholder ->
                    oldItem.id == newItem.id
                else -> false
            }

            override fun areContentsTheSame(
                oldItem: GoogleSearchFeedItem,
                newItem: GoogleSearchFeedItem
            ): Boolean = oldItem == newItem
        }
    }
}
