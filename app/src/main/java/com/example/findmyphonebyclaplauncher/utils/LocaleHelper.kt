package com.example.findmyphonebyclaplauncher.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.example.findmyphonebyclaplauncher.data.local.UserPreferencesDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

object LocaleHelper {

    private val _languageFlow = MutableStateFlow("en")
    val languageFlow: StateFlow<String> = _languageFlow.asStateFlow()

    fun onAttach(context: Context): Context {
        val lang = getLocale(context)
        _languageFlow.value = lang
        return updateResources(context, lang)
    }

    fun setLocale(context: Context, languageCode: String): Context {
        persist(context, languageCode)
        _languageFlow.value = languageCode
        return updateResources(context, languageCode)
    }

    fun getLocale(context: Context): String {
        return UserPreferencesDataSource(context).selectedLanguage
    }

    fun getLocaleObject(languageCode: String): Locale {
        return when (languageCode.lowercase()) {
            "en" -> Locale.ENGLISH
            "zh" -> Locale.SIMPLIFIED_CHINESE
            "in" -> Locale.forLanguageTag("id-ID")
            else -> Locale.forLanguageTag(languageCode)
        }
    }

    fun applyLocaleToContext(context: Context, languageCode: String) {
        val locale = getLocaleObject(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val config = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLayoutDirection(locale)

        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        val appContext = context.applicationContext
        if (appContext != null && appContext != context) {
            @Suppress("DEPRECATION")
            appContext.resources.updateConfiguration(config, appContext.resources.displayMetrics)
        }
    }

    private fun persist(context: Context, languageCode: String) {
        UserPreferencesDataSource(context).selectedLanguage = languageCode
    }

    private fun updateResources(context: Context, languageCode: String): Context {
        applyLocaleToContext(context, languageCode)
        val locale = getLocaleObject(languageCode)
        val config = Configuration(context.resources.configuration)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList(locale))
        } else {
            @Suppress("DEPRECATION")
            config.locale = locale
        }
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}


