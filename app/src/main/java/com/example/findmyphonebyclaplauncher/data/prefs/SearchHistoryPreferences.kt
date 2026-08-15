package com.example.findmyphonebyclaplauncher.data.prefs

import android.content.Context

class SearchHistoryPreferences(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getRecent(): List<String> {
        val raw = prefs.getString(KEY_RECENT, "").orEmpty()
        if (raw.isBlank()) return emptyList()
        return raw.split(SEPARATOR).map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun addRecent(query: String) {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return
        val updated = (listOf(cleaned) + getRecent().filter { !it.equals(cleaned, ignoreCase = true) })
            .take(MAX_RECENT)
        prefs.edit().putString(KEY_RECENT, updated.joinToString(SEPARATOR)).apply()
    }

    fun clearRecent() {
        prefs.edit().remove(KEY_RECENT).apply()
    }

    companion object {
        private const val PREFS = "google_search_history"
        private const val KEY_RECENT = "recent_queries"
        private const val SEPARATOR = "|||"
        private const val MAX_RECENT = 10
    }
}
