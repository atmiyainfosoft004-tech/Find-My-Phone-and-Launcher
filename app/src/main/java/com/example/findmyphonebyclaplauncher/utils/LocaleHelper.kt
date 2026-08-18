package com.example.findmyphonebyclaplauncher.utils

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import java.util.Locale

object LocaleHelper {

    fun onAttach(context: Context): Context {
        val lang = getLocale(context)
        return setLocale(context, lang)
    }

    fun setLocale(context: Context, languageCode: String): Context {
        persist(context, languageCode)
        runCatching {
            val appLocales = LocaleListCompat.forLanguageTags(languageCode)
            AppCompatDelegate.setApplicationLocales(appLocales)
        }
        return updateResources(context, languageCode)
    }

    fun getLocale(context: Context): String {
        return UserPreferencesDataSource(context).selectedLanguage
    }

    private fun persist(context: Context, languageCode: String) {
        UserPreferencesDataSource(context).selectedLanguage = languageCode
    }

    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }
}
