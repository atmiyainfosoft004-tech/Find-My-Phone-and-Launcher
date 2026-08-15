package com.example.findmyphonebyclaplauncher.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object GreetingHelper {

    fun greeting(): String {
        return when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..21 -> "Good Evening"
            else -> "Good Night"
        }
    }

    fun formattedDate(): String {
        val sdf = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())
        return sdf.format(Calendar.getInstance().time)
    }
}
