package com.example.findmyphonebyclaplauncher.ui.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.findmyphonebyclaplauncher.ads.config.AdsConfigManager
import com.example.findmyphonebyclaplauncher.data.model.BlogPost
import com.example.findmyphonebyclaplauncher.data.model.GoogleSearchFeedItem
import com.example.findmyphonebyclaplauncher.data.prefs.SearchHistoryPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoogleSearchViewModel(application: Application) : AndroidViewModel(application) {

    private val searchHistory = SearchHistoryPreferences(application)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _recent = MutableStateFlow(searchHistory.getRecent())
    val recent: StateFlow<List<String>> = _recent.asStateFlow()

    private val _suggestions = MutableStateFlow<List<String>>(emptyList())
    val suggestions: StateFlow<List<String>> = _suggestions.asStateFlow()

    private val _feed = MutableStateFlow(buildFeed(MOCK_BLOGS))
    val feed: StateFlow<List<GoogleSearchFeedItem>> = _feed.asStateFlow()

    fun onQueryChanged(text: String) {
        _query.value = text
        val q = text.trim()
        _suggestions.value = if (q.isEmpty()) {
            emptyList()
        } else {
            MOCK_SUGGESTIONS.filter { it.contains(q, ignoreCase = true) }.take(10)
        }
    }

    fun refreshRecent() {
        _recent.value = searchHistory.getRecent()
    }

    fun clearRecent() {
        searchHistory.clearRecent()
        _recent.value = emptyList()
    }

    fun rememberSearch(query: String) {
        searchHistory.addRecent(query)
        _recent.value = searchHistory.getRecent()
    }

    fun googleSearchUrl(query: String): String {
        val encoded = java.net.URLEncoder.encode(query.trim(), Charsets.UTF_8.name())
        return "https://www.google.com/search?q=$encoded"
    }

    companion object {
        private const val ADS_EVERY_N_BLOGS = 3

        fun buildFeed(
            blogs: List<BlogPost>,
            insertAds: Boolean = AdsConfigManager.config.canShowNative
        ): List<GoogleSearchFeedItem> {
            val items = mutableListOf<GoogleSearchFeedItem>()
            blogs.forEachIndexed { index, post ->
                items += GoogleSearchFeedItem.Blog(post)
                if (insertAds && (index + 1) % ADS_EVERY_N_BLOGS == 0) {
                    items += GoogleSearchFeedItem.AdPlaceholder((index + 1) / ADS_EVERY_N_BLOGS)
                }
            }
            return items
        }

        private val MOCK_SUGGESTIONS = listOf(
            "free fire",
            "frido",
            "free job alert",
            "freefast",
            "free fire name",
            "free games",
            "from season 4",
            "free ai video generator",
            "free fire download",
            "freelancer",
            "weather today",
            "news headlines",
            "best android launcher"
        )

        val MOCK_BLOGS = listOf(
            BlogPost(
                id = "1",
                title = "Forget chatbot training. AI's next big data grab is about learning how humans work.",
                source = "businessinsider.com",
                timeLabel = "2:30pm, Wed",
                url = "https://www.businessinsider.com",
                imageColor = 0xFF3D5A80.toInt()
            ),
            BlogPost(
                id = "2",
                title = "Inflation is expected to cool again in today's July CPI report",
                source = "businessinsider.com",
                timeLabel = "2:30pm, Wed",
                url = "https://www.businessinsider.com",
                imageColor = 0xFF2F3E46.toInt()
            ),
            BlogPost(
                id = "3",
                title = "FBI touts 6,000 arrests, 44% homicide drop one year into Trump's DC crime crackdown",
                source = "foxnews.com",
                timeLabel = "2:30pm, Wed",
                url = "https://www.foxnews.com",
                imageColor = 0xFF4A5568.toInt()
            ),
            BlogPost(
                id = "4",
                title = "How new Android launchers are changing the home screen experience in 2026",
                source = "androidauthority.com",
                timeLabel = "1:10pm, Wed",
                url = "https://www.androidauthority.com",
                imageColor = 0xFF1B4332.toInt()
            ),
            BlogPost(
                id = "5",
                title = "5 ways to cut phone screen time without deleting your favorite apps",
                source = "techcrunch.com",
                timeLabel = "11:45am, Wed",
                url = "https://techcrunch.com",
                imageColor = 0xFF5C4D7A.toInt()
            ),
            BlogPost(
                id = "6",
                title = "Call blocking tips that actually reduce spam without missing important numbers",
                source = "theverge.com",
                timeLabel = "10:20am, Wed",
                url = "https://www.theverge.com",
                imageColor = 0xFF3A506B.toInt()
            ),
            BlogPost(
                id = "7",
                title = "Why privacy-first dialers are gaining popularity on Google Play",
                source = "9to5google.com",
                timeLabel = "9:05am, Wed",
                url = "https://9to5google.com",
                imageColor = 0xFF264653.toInt()
            ),
            BlogPost(
                id = "8",
                title = "Smart search features users expect from a modern Android launcher",
                source = "androidpolice.com",
                timeLabel = "8:40am, Wed",
                url = "https://www.androidpolice.com",
                imageColor = 0xFF6D597A.toInt()
            ),
            BlogPost(
                id = "9",
                title = "Battery myths that still waste hours of phone life every week",
                source = "wired.com",
                timeLabel = "7:15am, Wed",
                url = "https://www.wired.com",
                imageColor = 0xFF415A77.toInt()
            )
        )
    }
}
