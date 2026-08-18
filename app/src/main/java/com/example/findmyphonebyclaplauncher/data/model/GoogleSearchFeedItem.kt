package com.example.findmyphonebyclaplauncher.data.model

sealed class GoogleSearchFeedItem {
    data class FeedItem(val post: BlogPost) : GoogleSearchFeedItem()
    data class NativeAd(val id: Int) : GoogleSearchFeedItem()
}

// Backward compatibility typealiases
typealias Blog = GoogleSearchFeedItem.FeedItem
typealias AdPlaceholder = GoogleSearchFeedItem.NativeAd
