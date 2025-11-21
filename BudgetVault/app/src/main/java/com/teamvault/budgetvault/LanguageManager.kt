package com.teamvault.budgetvault

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LanguageManager {
    private const val LANGUAGE_PREFERENCE = "AppLanguagePreference"
    private const val SELECTED_LANGUAGE = "SelectedLanguage"
    private const val FIRST_LAUNCH = "FirstLaunch"

    fun setLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(LANGUAGE_PREFERENCE, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(SELECTED_LANGUAGE, languageCode)
            .putBoolean(FIRST_LAUNCH, false)
            .apply()
    }

    fun getLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(LANGUAGE_PREFERENCE, Context.MODE_PRIVATE)

        // Check if it's the first launch
        val isFirstLaunch = prefs.getBoolean(FIRST_LAUNCH, true)

        // If it's first launch, return English and mark first launch as done
        if (isFirstLaunch) {
            prefs.edit()
                .putBoolean(FIRST_LAUNCH, false)
                .putString(SELECTED_LANGUAGE, "en")
                .apply()
            return "en"
        }

        // Otherwise, return the saved language or default to English
        return prefs.getString(SELECTED_LANGUAGE, "en") ?: "en"
    }

    fun applyLanguage(context: Context): Context {
        val language = getLanguage(context)
        val locale = Locale(language)
        Locale.setDefault(locale)

        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(locale)

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(configuration)
        } else {
            configuration.setLayoutDirection(locale)
            context.resources.updateConfiguration(configuration, context.resources.displayMetrics)
            context
        }
    }

    fun updateLanguage(context: Context, languageCode: String) {
        setLanguage(context, languageCode)
        applyLanguage(context)
    }
}