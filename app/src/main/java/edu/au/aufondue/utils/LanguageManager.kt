package edu.au.aufondue.utils

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.util.*

object LanguageManager {
    private const val PREF_NAME = "language_preferences"
    private const val KEY_LANGUAGE = "selected_language"

    const val ENGLISH = "en"
    const val THAI = "th"

    fun setLocale(context: Context, languageCode: String): Context {
        saveLanguagePreference(context, languageCode)
        return updateResources(context, languageCode)
    }

    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, ENGLISH) ?: ENGLISH // Default to English
    }

    // Add the missing methods
    fun applyLanguage(context: Context) {
        val savedLanguage = getSavedLanguage(context)
        updateResources(context, savedLanguage)
    }

    fun getSavedLanguage(context: Context): String {
        return getSelectedLanguage(context)
    }

    private fun saveLanguagePreference(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    private fun updateResources(context: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config: Configuration = resources.configuration
        config.setLocale(locale)

        return context.createConfigurationContext(config)
    }

    fun getDisplayName(languageCode: String): String {
        return when (languageCode) {
            ENGLISH -> "English"
            THAI -> "ไทย"
            else -> "English"
        }
    }
}