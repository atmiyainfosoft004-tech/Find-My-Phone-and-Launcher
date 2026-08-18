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
    fun buildFeed_withMandatoryAdAndInterval4_insertsInitialAdAt1AndIntervalAdsEvery4Items() {
        val blogs = GoogleSearchViewModel.MOCK_BLOGS // 9 items
        val feed = GoogleSearchViewModel.buildFeed(
            blogs = blogs,
            insertAds = true,
            interval = 4
        )

        // 9 items: Position 0 = Item 0
        // Position 1 = Mandatory Initial Ad
        // Items 1..8 (8 remaining items): Interval 4 -> ads after item 4 and item 8 (2 interval ads)
        // Total = 9 items + 1 mandatory ad + 2 interval ads = 12 items
        assertEquals(12, feed.size)

        // Position 0: Primary FeedItem
        assertTrue(feed[0] is GoogleSearchFeedItem.FeedItem)
        // Position 1: Mandatory Initial NativeAd
        assertTrue(feed[1] is GoogleSearchFeedItem.NativeAd)
        // Position 2..5: FeedItems 1..4
        assertTrue(feed[2] is GoogleSearchFeedItem.FeedItem)
        assertTrue(feed[3] is GoogleSearchFeedItem.FeedItem)
        assertTrue(feed[4] is GoogleSearchFeedItem.FeedItem)
        assertTrue(feed[5] is GoogleSearchFeedItem.FeedItem)
        // Position 6: First Interval NativeAd
        assertTrue(feed[6] is GoogleSearchFeedItem.NativeAd)
        // Position 7..10: FeedItems 5..8
        assertTrue(feed[7] is GoogleSearchFeedItem.FeedItem)
        assertTrue(feed[8] is GoogleSearchFeedItem.FeedItem)
        assertTrue(feed[9] is GoogleSearchFeedItem.FeedItem)
        assertTrue(feed[10] is GoogleSearchFeedItem.FeedItem)
        // Position 11: Second Interval NativeAd
        assertTrue(feed[11] is GoogleSearchFeedItem.NativeAd)
    }

    @Test
    fun parseFeedListConfig_withValidJson_parsesCorrectly() {
        val json = """[
            {
                "id": "100",
                "heading": "Remote Config Heading",
                "description": "Remote Config Description",
                "source": "remote.com",
                "timeLabel": "Just now",
                "feedurl": "https://remote.com/news",
                "img_url": "https://remote.com/image.png"
            }
        ]"""

        val parsed = GoogleSearchViewModel.parseFeedListConfig(json)
        assertEquals(1, parsed.size)
        assertEquals("100", parsed[0].id)
        assertEquals("Remote Config Heading", parsed[0].title)
        assertEquals("https://remote.com/news", parsed[0].url)
    }

    @Test
    fun buildFeed_withAdsDisabled_containsZeroAds() {
        val blogs = GoogleSearchViewModel.MOCK_BLOGS
        val feed = GoogleSearchViewModel.buildFeed(
            blogs = blogs,
            insertAds = false,
            interval = 4
        )

        assertEquals(blogs.size, feed.size)
        assertTrue(feed.none { it is GoogleSearchFeedItem.NativeAd })
    }
}
