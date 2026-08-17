package com.example.findmyphonebyclaplauncher.ads

import com.example.findmyphonebyclaplauncher.data.model.GoogleSearchFeedItem
import com.example.findmyphonebyclaplauncher.ui.search.GoogleSearchViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for Google Search screen dynamic native ad insertion intervals.
 */
class GoogleSearchFeedAdIntervalTest {

    @Test
    fun buildFeed_withInterval2_insertsAdsEvery2Items() {
        val blogs = GoogleSearchViewModel.MOCK_BLOGS // 9 blogs
        val feed = GoogleSearchViewModel.buildFeed(
            blogs = blogs,
            insertAds = true,
            interval = 2
        )

        // 9 blogs with interval 2 -> ads after blog 2, 4, 6, 8 (4 ads total) -> total 13 items
        assertEquals(13, feed.size)

        // Position 0: Blog 1
        assertTrue(feed[0] is GoogleSearchFeedItem.Blog)
        // Position 1: Blog 2
        assertTrue(feed[1] is GoogleSearchFeedItem.Blog)
        // Position 2: Ad 1
        assertTrue(feed[2] is GoogleSearchFeedItem.AdPlaceholder)
        // Position 3: Blog 3
        assertTrue(feed[3] is GoogleSearchFeedItem.Blog)
        // Position 4: Blog 4
        assertTrue(feed[4] is GoogleSearchFeedItem.Blog)
        // Position 5: Ad 2
        assertTrue(feed[5] is GoogleSearchFeedItem.AdPlaceholder)
    }

    @Test
    fun buildFeed_withAdsDisabled_containsZeroAds() {
        val blogs = GoogleSearchViewModel.MOCK_BLOGS
        val feed = GoogleSearchViewModel.buildFeed(
            blogs = blogs,
            insertAds = false,
            interval = 2
        )

        assertEquals(blogs.size, feed.size)
        assertTrue(feed.none { it is GoogleSearchFeedItem.AdPlaceholder })
    }

    @Test
    fun buildFeed_withInterval3_insertsAdsEvery3Items() {
        val blogs = GoogleSearchViewModel.MOCK_BLOGS // 9 blogs
        val feed = GoogleSearchViewModel.buildFeed(
            blogs = blogs,
            insertAds = true,
            interval = 3
        )

        // 9 blogs with interval 3 -> ads after blog 3, 6, 9 (3 ads total) -> total 12 items
        assertEquals(12, feed.size)
        assertTrue(feed[3] is GoogleSearchFeedItem.AdPlaceholder)
        assertTrue(feed[7] is GoogleSearchFeedItem.AdPlaceholder)
        assertTrue(feed[11] is GoogleSearchFeedItem.AdPlaceholder)
    }
}
