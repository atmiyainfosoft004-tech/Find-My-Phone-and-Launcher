package com.example.findmyphonebyclaplauncher.data.model

sealed class GoogleSearchFeedItem {
    data class Blog(val post: BlogPost) : GoogleSearchFeedItem()
    data class AdPlaceholder(val id: Int) : GoogleSearchFeedItem()
}
