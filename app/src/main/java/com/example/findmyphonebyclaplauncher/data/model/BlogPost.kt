package com.example.findmyphonebyclaplauncher.data.model

import com.google.gson.annotations.SerializedName

data class FeedConfigResponse(
    @SerializedName("feedlist") val feedlist: List<FeedItem> = emptyList()
)

data class FeedItem(
    @SerializedName("id") val id: String = "",
    @SerializedName("heading", alternate = ["title"]) val heading: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("source") val source: String = "google.com",
    @SerializedName("timeLabel") val timeLabel: String = "Top Story",
    @SerializedName("feedurl", alternate = ["url"]) val feedurl: String = "",
    @SerializedName("imageColor") val imageColor: Int = 0xFF3D5A80.toInt(),
    @SerializedName("img_url", alternate = ["imageUrl"]) val img_url: String = ""
) {
    constructor(
        id: String,
        title: String,
        source: String,
        timeLabel: String,
        url: String,
        imageColor: Int
    ) : this(
        id = id,
        heading = title,
        description = "",
        source = source,
        timeLabel = timeLabel,
        feedurl = url,
        imageColor = imageColor,
        img_url = ""
    )

    val title: String get() = heading.ifBlank { "Top Story" }
    val url: String get() = feedurl
    val imageUrl: String get() = img_url
}

typealias BlogPost = FeedItem
