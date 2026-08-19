package com.example.findmyphonebyclaplauncher.ui.common

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.findmyphonebyclaplauncher.utils.LocaleHelper
import kotlinx.coroutines.launch

abstract class BaseActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        val contextWithLocale = LocaleHelper.onAttach(newBase)
        super.attachBaseContext(contextWithLocale)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val lang = LocaleHelper.getLocale(this)
        LocaleHelper.applyLocaleToContext(this, lang)
        super.onCreate(savedInstanceState)
        observeLanguageChanges()
    }

    private fun observeLanguageChanges() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                LocaleHelper.languageFlow.collect { lang ->
                    LocaleHelper.applyLocaleToContext(this@BaseActivity, lang)
                    updateLocalizedTexts()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val lang = LocaleHelper.getLocale(this)
        LocaleHelper.applyLocaleToContext(this, lang)
        updateLocalizedTexts()
    }

    /**
     * Override in child activities to re-bind localized strings dynamically when the locale changes or activity resumes.
     */
    open fun updateLocalizedTexts() {}
}

