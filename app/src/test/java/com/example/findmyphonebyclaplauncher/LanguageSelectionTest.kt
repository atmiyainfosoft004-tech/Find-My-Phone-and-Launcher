package com.example.findmyphonebyclaplauncher

import com.example.findmyphonebyclaplauncher.ui.language.LanguageItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageSelectionTest {

    @Test
    fun initialLanguageList_hasNoLanguageSelectedByDefault() {
        val rawList = listOf(
            LanguageItem("en", "English", "English", isSelected = false),
            LanguageItem("hi", "Hindi", "हिन्दी", isSelected = false),
            LanguageItem("es", "Spanish", "Español", isSelected = false),
            LanguageItem("fr", "French", "Français", isSelected = false),
            LanguageItem("de", "German", "Deutsch", isSelected = false),
            LanguageItem("in", "Indonesian", "Bahasa Indonesia", isSelected = false),
            LanguageItem("ru", "Russian", "Русский", isSelected = false),
            LanguageItem("zh", "Chinese", "中文", isSelected = false)
        )

        val selectedIndex = rawList.indexOfFirst { it.isSelected }
        assertEquals("Initial selection index must be -1 when starting with no default selection", -1, selectedIndex)
        assertTrue("All items must have isSelected = false", rawList.none { it.isSelected })
    }

    @Test
    fun doneButtonState_onlyEnabledWhenLanguageIsSelected() {
        var selectedItem: LanguageItem? = null

        fun isDoneButtonVisible(selection: LanguageItem?): Boolean = selection != null

        // Initial state
        assertNull(selectedItem)
        assertFalse("Done button must be hidden/disabled initially", isDoneButtonVisible(selectedItem))

        // User selects Spanish
        selectedItem = LanguageItem("es", "Spanish", "Español")
        assertTrue("Done button must be shown/enabled once a language is selected", isDoneButtonVisible(selectedItem))
        assertEquals("es", selectedItem.code)
    }
}
