package com.example.findmyphonebyclaplauncher.ui.language

data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    var isSelected: Boolean = false
)
