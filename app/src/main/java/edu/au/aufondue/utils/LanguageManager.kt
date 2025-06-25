// Location: app/src/main/java/edu/au/aufondue/utils/LanguageManager.kt
// THIS IS A NEW FILE - CREATE THIS FILE

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

    fun setLocale(context: Context, languageCode: String) {
        saveLanguagePreference(context, languageCode)
        updateResources(context, languageCode)
    }

    fun getSelectedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, ENGLISH) ?: ENGLISH // Default to English
    }

    private fun saveLanguagePreference(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }

    private fun updateResources(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources: Resources = context.resources
        val config: Configuration = resources.configuration
        config.setLocale(locale)

        context.createConfigurationContext(config)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    fun getDisplayName(languageCode: String): String {
        return when (languageCode) {
            ENGLISH -> "English"
            THAI -> "ไทย"
            else -> "English"
        }
    }
}