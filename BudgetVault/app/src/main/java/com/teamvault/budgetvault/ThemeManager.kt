package com.teamvault.budgetvault

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

/**
 * ThemeManager to handle saving and applying theme preferences
 */
object ThemeManager {
    private const val PREF_NAME = "theme_preferences"
    private const val KEY_DARK_THEME = "is_dark_theme"

    /**
     * Get the SharedPreferences instance
     */
    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Check if dark theme is currently enabled
     */
    fun isDarkTheme(context: Context): Boolean {
        return getPreferences(context).getBoolean(KEY_DARK_THEME, false)
    }

    /**
     * Set and apply theme
     */
    fun setTheme(context: Context, isDarkTheme: Boolean) {
        // Save preference
        getPreferences(context).edit().putBoolean(KEY_DARK_THEME, isDarkTheme).apply()

        // Apply theme
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    /**
     * Apply the saved theme (call this in BaseActivity or Application)
     */
    fun applyTheme(context: Context) {
        val isDarkTheme = isDarkTheme(context)
        if (isDarkTheme) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}