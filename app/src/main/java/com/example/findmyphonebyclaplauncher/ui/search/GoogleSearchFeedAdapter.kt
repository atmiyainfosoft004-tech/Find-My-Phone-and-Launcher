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

typealias FeedAdapter = GoogleSearchFeedAdapter

class GoogleSearchFeedAdapter(
    private val activity: Activity,
    private val onBlogClick: (BlogPost) -> Unit
) : ListAdapter<GoogleSearchFeedItem, RecyclerView.ViewHolder>(DIFF) {

    private val nativeAds = mutableMapOf<Int, NativeAd>()
    private val loadingIds = mutableSetOf<Int>()
    private val failedIds = mutableSetOf<Int>()
    private var destroyed = false

    override fun getItemViewType(position: Int): Int {
        if (position < 0 || position >= currentList.size) return VIEW_TYPE_FEED_ITEM
        return when (getItem(position)) {
            is GoogleSearchFeedItem.FeedItem -> VIEW_TYPE_FEED_ITEM
            is GoogleSearchFeedItem.NativeAd -> VIEW_TYPE_NATIVE_AD
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_FEED_ITEM -> BlogVH(ItemBlogPostBinding.inflate(inflater, parent, false))
            else -> AdVH(ItemBlogAdPlaceholderBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val dataIndex = adapterPositionToDataIndex(position)
        when (val item = getItem(position)) {
            is GoogleSearchFeedItem.FeedItem -> (holder as BlogVH).bind(item.post, onBlogClick)
            is GoogleSearchFeedItem.NativeAd -> (holder as AdVH).bind(item.id)
        }
    }

    /**
     * Maps a RecyclerView adapter position to the raw feed data index.
     * Returns -1 if position is an ad or invalid.
     */
    fun adapterPositionToDataIndex(position: Int): Int {
        if (position < 0 || position >= currentList.size) return -1
        if (getItem(position) is GoogleSearchFeedItem.NativeAd) return -1
        val adsBefore = currentList.take(position).count { it is GoogleSearchFeedItem.NativeAd }
        return position - adsBefore
    }

    /**
     * Maps a raw feed data index to its corresponding RecyclerView adapter position.
     * Returns -1 if not found.
     */
    fun dataIndexToAdapterPosition(dataIndex: Int): Int {
        var feedCount = 0
        currentList.forEachIndexed { index, item ->
            if (item is GoogleSearchFeedItem.FeedItem) {
                if (feedCount == dataIndex) return index
                feedCount++
            }
        }
        return -1
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
        if (!com.example.findmyphonebyclaplauncher.util.NetworkUtil.isNetworkAvailable(activity) ||
            !AdsConfigManager.config.canShowNativeGoogleSearch ||
            adId in failedIds
        ) {
            hideSlot()
            return
        }

        val cached = nativeAds[adId]
        if (cached != null) {
            binding.root.visibility = View.VISIBLE
            LauncherAdsHelper.bindGoogleSearchNative(
                activity,
                binding.nativeAdFrameLayout,
                binding.nativeAdShimmerFrameLayout,
                cached
            )
            return
        }

        binding.root.visibility = View.VISIBLE
        binding.nativeAdShimmerFrameLayout.visibility = View.VISIBLE
        binding.nativeAdFrameLayout.visibility = View.GONE
        binding.nativeAdFrameLayout.removeAllViews()

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
                if (boundAdId == adId) {
                    LauncherAdsHelper.bindGoogleSearchNative(
                        activity,
                        binding.nativeAdFrameLayout,
                        binding.nativeAdShimmerFrameLayout,
                        nativeAd
                    )
                } else {
                    notifyAdSlot(adId)
                }
            },
            onFailed = {
                if (destroyed) return@loadGoogleSearchNative
                loadingIds -= adId
                failedIds += adId
                if (boundAdId == adId) {
                    hideSlot()
                } else {
                    notifyAdSlot(adId)
                }
            }
        )
    }

    private fun notifyAdSlot(adId: Int) {
        val position = currentList.indexOfFirst {
            it is GoogleSearchFeedItem.NativeAd && it.id == adId
        }
        if (position >= 0) notifyItemChanged(position)
    }

    class BlogVH(private val binding: ItemBlogPostBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(post: BlogPost, onClick: (BlogPost) -> Unit) {
            binding.tvTitle.text = post.heading.ifBlank { post.title }

            val displaySource = post.source.takeIf {
                it.isNotBlank() && !it.equals("google.com", ignoreCase = true)
            } ?: runCatching {
                val host = android.net.Uri.parse(post.feedurl.ifBlank { post.url }).host?.removePrefix("www.")
                if (!host.isNullOrBlank()) host else "google.com"
            }.getOrDefault("google.com")

            binding.tvSource.text = displaySource

            val displayTime = post.timeLabel.takeIf {
                it.isNotBlank() && !it.equals("Top Story", ignoreCase = true)
            }
            if (displayTime.isNullOrBlank()) {
                binding.tvTime.visibility = View.GONE
                binding.ivClock.visibility = View.GONE
            } else {
                binding.tvTime.visibility = View.VISIBLE
                binding.ivClock.visibility = View.VISIBLE
                binding.tvTime.text = displayTime
            }

            val dynamicColor = if (post.imageColor != 0 && post.imageColor != 0xFF3D5A80.toInt()) {
                post.imageColor
            } else {
                val colors = intArrayOf(
                    0xFF3D5A80.toInt(), 0xFF2F3E46.toInt(), 0xFF4A5568.toInt(),
                    0xFF1B4332.toInt(), 0xFF5C4D7A.toInt(), 0xFF3A506B.toInt(),
                    0xFF264653.toInt(), 0xFF6D597A.toInt(), 0xFF415A77.toInt()
                )
                val hash = kotlin.math.abs((post.heading + post.feedurl).hashCode())
                colors[hash % colors.size]
            }

            val placeholderDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 12f * binding.root.resources.displayMetrics.density
                setColor(dynamicColor)
            }

            val imgUrl = post.img_url.ifBlank { post.imageUrl }
            if (imgUrl.isNotBlank()) {
                com.bumptech.glide.Glide.with(binding.ivCover.context)
                    .load(imgUrl)
                    .placeholder(placeholderDrawable)
                    .error(placeholderDrawable)
                    .centerCrop()
                    .into(binding.ivCover)
            } else {
                com.bumptech.glide.Glide.with(binding.ivCover.context).clear(binding.ivCover)
                binding.ivCover.setImageDrawable(null)
                binding.ivCover.background = placeholderDrawable
            }

            val dot = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 3f * binding.root.resources.displayMetrics.density
                setColor(ColorUtils.blendARGB(dynamicColor, 0xFF4285F4.toInt(), 0.35f))
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
        const val VIEW_TYPE_FEED_ITEM = 1
        const val VIEW_TYPE_NATIVE_AD = 2

        private val DIFF = object : DiffUtil.ItemCallback<GoogleSearchFeedItem>() {
            override fun areItemsTheSame(
                oldItem: GoogleSearchFeedItem,
                newItem: GoogleSearchFeedItem
            ): Boolean = when {
                oldItem is GoogleSearchFeedItem.FeedItem && newItem is GoogleSearchFeedItem.FeedItem ->
                    oldItem.post.id == newItem.post.id
                oldItem is GoogleSearchFeedItem.NativeAd && newItem is GoogleSearchFeedItem.NativeAd ->
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
