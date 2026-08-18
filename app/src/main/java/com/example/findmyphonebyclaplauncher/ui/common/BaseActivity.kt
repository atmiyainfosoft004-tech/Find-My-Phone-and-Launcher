package com.example.findmyphonebyclaplauncher.ui.common

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.findmyphonebyclaplauncher.utils.LocaleHelper

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val contextWithLocale = LocaleHelper.onAttach(newBase)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onResume() {
        super.onResume()
        updateLocalizedTexts()
    }

    /**
     * Override in child activities to re-bind localized strings dynamically when the locale changes or activity resumes.
     */
    open fun updateLocalizedTexts() {}
}
